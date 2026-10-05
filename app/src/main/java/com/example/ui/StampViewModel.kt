package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.GoogleAuthHelper
import com.example.data.DailyStamp
import com.example.data.StampDatabase
import com.example.data.StampRepository
import com.example.data.firebase.FirebaseStampRepository
import com.example.data.firebase.UserProfile
import com.example.util.ImageFileHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StampViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepository: StampRepository
    private val firebaseRepository: FirebaseStampRepository
    val authHelper: GoogleAuthHelper = GoogleAuthHelper(application)

    val currentUser: StateFlow<FirebaseUser?> = authHelper.authStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authHelper.currentUser
        )

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        val stampDao = StampDatabase.getDatabase(application).stampDao()
        localRepository = StampRepository(stampDao)
        firebaseRepository = FirebaseStampRepository(application)

        // Observe auth changes and sync streaks to Firebase
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    syncStreaksToFirebase(user)
                    // Observe cloud user profile
                    firebaseRepository.observeUserProfile(user.uid).collect { profile ->
                        _userProfile.value = profile
                    }
                } else {
                    _userProfile.value = null
                }
            }
        }

        checkAndSeedSampleData(application)
    }

    val allStamps: StateFlow<List<DailyStamp>> = localRepository.allStamps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Calendar Navigation State
    private val _calendarMonth = MutableStateFlow(Calendar.getInstance())
    val calendarMonth: StateFlow<Calendar> = _calendarMonth.asStateFlow()

    fun nextMonth() {
        val cal = (_calendarMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _calendarMonth.value = cal
    }

    fun previousMonth() {
        val cal = (_calendarMonth.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _calendarMonth.value = cal
    }

    fun formatCurrentMonth(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        return sdf.format(_calendarMonth.value.time)
    }

    // Stamp Creation & Editing Form State
    val imageUriState = MutableStateFlow<String?>(null)
    val captionState = MutableStateFlow("")
    val dateMillisState = MutableStateFlow(System.currentTimeMillis())
    val dateFormattedState = MutableStateFlow(formatDate(System.currentTimeMillis()))
    val locationState = MutableStateFlow("")
    val selectedShapeType = MutableStateFlow("PORTRAIT") // CONTOUR, PORTRAIT, SQUARE, CIRCLE
    val isPublicState = MutableStateFlow(false)

    // Currently viewed stamp for detail modal
    private val _selectedDetailStamp = MutableStateFlow<DailyStamp?>(null)
    val selectedDetailStamp: StateFlow<DailyStamp?> = _selectedDetailStamp.asStateFlow()

    var tempCameraUri: Uri? = null

    fun selectDetailStamp(stamp: DailyStamp?) {
        _selectedDetailStamp.value = stamp
    }

    fun onImageSelected(uri: Uri, context: Context) {
        viewModelScope.launch {
            val permanentUri = ImageFileHelper.persistImageToAppStorage(context, uri)
            imageUriState.value = permanentUri
        }
    }

    fun onCaptionChanged(newCaption: String) {
        if (newCaption.length <= 280) {
            captionState.value = newCaption
        }
    }

    fun onLocationChanged(newLocation: String) {
        locationState.value = newLocation
    }

    fun onDateChanged(millis: Long) {
        dateMillisState.value = millis
        dateFormattedState.value = formatDate(millis)
    }

    fun setShapeType(shape: String) {
        selectedShapeType.value = shape
    }

    fun togglePublic(isPublic: Boolean) {
        isPublicState.value = isPublic
    }

    fun resetCreationForm() {
        imageUriState.value = null
        captionState.value = ""
        val now = System.currentTimeMillis()
        dateMillisState.value = now
        dateFormattedState.value = formatDate(now)
        locationState.value = ""
        selectedShapeType.value = "PORTRAIT"
        isPublicState.value = false
    }

    fun saveNewStamp(onSaved: (DailyStamp) -> Unit) {
        val uri = imageUriState.value ?: return
        viewModelScope.launch {
            val newStamp = DailyStamp(
                imageUri = uri,
                caption = captionState.value.trim(),
                dateMillis = dateMillisState.value,
                dateFormatted = dateFormattedState.value,
                locationOrTag = locationState.value.trim(),
                shapeType = selectedShapeType.value,
                isPublic = isPublicState.value
            )
            val insertedId = localRepository.insert(newStamp)
            val savedStamp = newStamp.copy(id = insertedId)

            // Link and sync to Firebase if user is logged in
            val user = currentUser.value
            if (user != null) {
                firebaseRepository.syncStamp(user.uid, savedStamp)
                syncStreaksToFirebase(user)
            }

            resetCreationForm()
            onSaved(savedStamp)
        }
    }

    fun toggleFavorite(stamp: DailyStamp) {
        viewModelScope.launch {
            localRepository.update(stamp.copy(isFavorite = !stamp.isFavorite))
            if (_selectedDetailStamp.value?.id == stamp.id) {
                _selectedDetailStamp.value = _selectedDetailStamp.value?.copy(isFavorite = !stamp.isFavorite)
            }
        }
    }

    fun deleteStamp(stamp: DailyStamp) {
        viewModelScope.launch {
            localRepository.delete(stamp)
            val user = currentUser.value
            if (user != null) {
                firebaseRepository.deleteStamp(user.uid, stamp.id)
                syncStreaksToFirebase(user)
            }
            if (_selectedDetailStamp.value?.id == stamp.id) {
                _selectedDetailStamp.value = null
            }
        }
    }

    // Google Sign-In & Sign-Out
    fun signInWithGoogle(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authHelper.signInWithGoogle()
            val user = result.getOrNull()
            if (user != null) {
                syncStreaksToFirebase(user)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun signOut() {
        authHelper.signOut()
    }

    // Calculate Streak from Stamps List
    fun calculateStreak(stamps: List<DailyStamp>): Int {
        if (stamps.isEmpty()) return 0
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
        val uniqueDays = stamps.map { dayFormat.format(Date(it.dateMillis)) }.distinct().sortedDescending()

        val todayStr = dayFormat.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dayFormat.format(cal.time)

        if (uniqueDays.isEmpty() || (uniqueDays.first() != todayStr && uniqueDays.first() != yesterdayStr)) {
            return 0
        }

        var streak = 1
        var currentCal = Calendar.getInstance()
        if (uniqueDays.first() == yesterdayStr) {
            currentCal = cal
        }

        for (i in 1 until uniqueDays.size) {
            currentCal.add(Calendar.DAY_OF_YEAR, -1)
            val expectedDay = dayFormat.format(currentCal.time)
            if (uniqueDays[i] == expectedDay) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private suspend fun syncStreaksToFirebase(user: FirebaseUser) {
        val stamps = localRepository.allStamps.firstOrNull() ?: emptyList()
        val currentStreak = calculateStreak(stamps)
        val totalCount = stamps.size
        firebaseRepository.syncUserAndStreak(user, currentStreak, totalCount)
    }

    private fun checkAndSeedSampleData(context: Context) {
        viewModelScope.launch {
            val stamps = localRepository.allStamps.firstOrNull() ?: emptyList()
            if (stamps.isEmpty()) {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH)

                val samples = listOf(
                    Triple(3, "🐧", android.graphics.Color.parseColor("#38BDF8")),
                    Triple(4, "⌨️", android.graphics.Color.parseColor("#475569")),
                    Triple(6, "🐶", android.graphics.Color.parseColor("#F59E0B")),
                    Triple(12, "☕", android.graphics.Color.parseColor("#1C1917")),
                    Triple(13, "🌸", android.graphics.Color.parseColor("#F472B6")),
                    Triple(20, "🥪", android.graphics.Color.parseColor("#EA580C")),
                    Triple(22, "🌃", android.graphics.Color.parseColor("#1E293B")),
                    Triple(25, "🌅", android.graphics.Color.parseColor("#FBBF24"))
                )

                for ((day, emoji, color) in samples) {
                    val sampleCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, currentMonth)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 12)
                    }
                    val uri = ImageFileHelper.createSamplePhoto(context, color, emoji)
                    val sampleStamp = DailyStamp(
                        imageUri = uri,
                        caption = "Moment on day $day",
                        dateMillis = sampleCal.timeInMillis,
                        dateFormatted = formatDate(sampleCal.timeInMillis),
                        shapeType = if (day % 3 == 0) "SQUARE" else if (day % 5 == 0) "CIRCLE" else "PORTRAIT"
                    )
                    localRepository.insert(sampleStamp)
                }
            }
        }
    }

    companion object {
        fun formatDate(millis: Long): String {
            val sdf = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.ENGLISH)
            return sdf.format(Date(millis))
        }
    }
}
