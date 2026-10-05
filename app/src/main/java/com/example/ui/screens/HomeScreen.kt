package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DailyStamp
import com.example.ui.StampViewModel
import com.example.ui.components.MiniStampCard
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: StampViewModel,
    allStamps: List<DailyStamp>,
    onStampClick: (DailyStamp) -> Unit,
    onDayClick: (Int) -> Unit,
    onOpenJournalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val monthTitle = remember(calendarMonth) { viewModel.formatCurrentMonth() }
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }

    // Map stamps by day of the month for the current year & month
    val stampsByDay = remember(allStamps, calendarMonth) {
        val targetYear = calendarMonth.get(Calendar.YEAR)
        val targetMonth = calendarMonth.get(Calendar.MONTH)
        val map = mutableMapOf<Int, DailyStamp>()

        allStamps.forEach { stamp ->
            val stampCal = Calendar.getInstance().apply { timeInMillis = stamp.dateMillis }
            if (stampCal.get(Calendar.YEAR) == targetYear && stampCal.get(Calendar.MONTH) == targetMonth) {
                val day = stampCal.get(Calendar.DAY_OF_MONTH)
                if (!map.containsKey(day)) {
                    map[day] = stamp
                }
            }
        }
        map
    }

    // Calculate calendar days layout
    val calendarDays = remember(calendarMonth) {
        val cal = calendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        Pair(startOffset, maxDays)
    }

    val (startOffset, maxDays) = calendarDays
    val totalSlots = startOffset + maxDays

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Header: User Profile, Sign in with Google, and Journal Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (currentUser != null) {
                        showProfileDialog = true
                    } else {
                        viewModel.signInWithGoogle { success ->
                            Toast.makeText(
                                context,
                                if (success) "Signed in with Google! Streaks synced to Firebase" else "Sign-in was cancelled",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                // Profile Avatar (Google Photo or Icon)
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser?.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    if (currentUser != null) {
                        Text(
                            text = "Welcome,",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = currentUser?.displayName ?: "Collector",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Text(
                            text = "Welcome,",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Sign in with Google",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Journal / List View Icon
            IconButton(
                onClick = onOpenJournalClick,
                modifier = Modifier.testTag("open_journal_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Journal",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Month Navigation: < Month Year >
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.previousMonth() },
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = monthTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { viewModel.nextMonth() },
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Weekday Letters: M  T  W  T  F  S  S
        val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekdays.forEach { dayLetter ->
                Text(
                    text = dayLetter,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Monthly Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(totalSlots) { index ->
                if (index < startOffset) {
                    Spacer(modifier = Modifier.aspectRatio(0.72f))
                } else {
                    val dayNumber = index - startOffset + 1
                    val stampForDay = stampsByDay[dayNumber]

                    Card(
                        modifier = Modifier
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (stampForDay != null) {
                                    onStampClick(stampForDay)
                                } else {
                                    onDayClick(dayNumber)
                                }
                            }
                            .testTag("calendar_day_$dayNumber"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "$dayNumber",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 2.dp)
                            )

                            if (stampForDay != null) {
                                MiniStampCard(
                                    stamp = stampForDay,
                                    modifier = Modifier
                                        .fillMaxWidth(0.88f)
                                        .aspectRatio(0.85f)
                                        .align(Alignment.Center)
                                        .padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile Dialog (Firebase Link & Streak Details)
    if (showProfileDialog && currentUser != null) {
        val user = currentUser!!
        val streak = userProfile?.currentStreak ?: viewModel.calculateStreak(allStamps)
        val bestStreak = userProfile?.bestStreak ?: streak

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "Firebase Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = "Logged in as ${user.displayName ?: user.email}")
                    Text(
                        text = user.email ?: "",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Current Streak:", fontWeight = FontWeight.SemiBold)
                        Text(text = "$streak days", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Best Streak:", fontWeight = FontWeight.SemiBold)
                        Text(text = "$bestStreak days", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Synced Stamps:", fontWeight = FontWeight.SemiBold)
                        Text(text = "${allStamps.size} stamps", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Linked to Firebase Cloud",
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut()
                        showProfileDialog = false
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
