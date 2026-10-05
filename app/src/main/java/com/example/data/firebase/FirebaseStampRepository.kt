package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.DailyStamp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseStampRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    UserProfile(
                        userId = snapshot.getString("userId") ?: userId,
                        displayName = snapshot.getString("displayName") ?: "",
                        email = snapshot.getString("email") ?: "",
                        photoUrl = snapshot.getString("photoUrl") ?: "",
                        currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0,
                        bestStreak = snapshot.getLong("bestStreak")?.toInt() ?: 0,
                        totalStamps = snapshot.getLong("totalStamps")?.toInt() ?: 0,
                        lastActiveDate = snapshot.getString("lastActiveDate") ?: ""
                    )
                } else {
                    null
                }
            }
    }

    suspend fun syncUserAndStreak(
        user: FirebaseUser,
        calculatedStreak: Int,
        totalStamps: Int
    ) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val userRef = db.collection("users").document(user.uid)

        try {
            val existingDoc = userRef.get().await()
            val bestStreak = maxOf(
                existingDoc.getLong("bestStreak")?.toInt() ?: 0,
                calculatedStreak
            )

            val data = mapOf(
                "userId" to user.uid,
                "displayName" to (user.displayName ?: "Stamp Collector"),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "currentStreak" to calculatedStreak,
                "bestStreak" to bestStreak,
                "totalStamps" to totalStamps,
                "lastActiveDate" to todayStr,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            userRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirebaseStampRepo", "Failed to sync user profile and streak", e)
        }
    }

    suspend fun syncStamp(userId: String, stamp: DailyStamp) {
        try {
            val stampDocRef = db.collection("users").document(userId)
                .collection("stamps").document(stamp.id.toString())

            val stampData = mapOf(
                "id" to stamp.id.toString(),
                "userId" to userId,
                "caption" to stamp.caption,
                "dateFormatted" to stamp.dateFormatted,
                "dateMillis" to stamp.dateMillis,
                "shapeType" to stamp.shapeType,
                "isPublic" to stamp.isPublic,
                "locationOrTag" to stamp.locationOrTag,
                "imageUri" to stamp.imageUri,
                "createdAt" to FieldValue.serverTimestamp()
            )

            stampDocRef.set(stampData, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirebaseStampRepo", "Failed to sync stamp to Firestore", e)
        }
    }

    suspend fun deleteStamp(userId: String, stampId: Long) {
        try {
            db.collection("users").document(userId)
                .collection("stamps").document(stampId.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseStampRepo", "Failed to delete stamp from Firestore", e)
        }
    }
}
