package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DailyStamp
import com.example.ui.StampViewModel
import com.example.ui.components.StampDetailBottomSheet
import com.example.ui.screens.AlbumScreen
import com.example.ui.screens.CreateStampScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ImageFileHelper
import java.util.Calendar

sealed class AppScreen {
    object Home : AppScreen()
    object Album : AppScreen()
    object Create : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StampApp()
            }
        }
    }
}

@Composable
fun StampApp(viewModel: StampViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val allStamps by viewModel.allStamps.collectAsState()
    val selectedDetailStamp by viewModel.selectedDetailStamp.collectAsState()

    // Photo picker launcher (0-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri, context)
            currentScreen = AppScreen.Create
        }
    }

    // Camera capture launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uri = viewModel.tempCameraUri
            if (uri != null) {
                viewModel.onImageSelected(uri, context)
                currentScreen = AppScreen.Create
            }
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val tempUri = ImageFileHelper.createCameraTempUri(context)
            viewModel.tempCameraUri = tempUri
            takePictureLauncher.launch(tempUri)
        } else {
            Toast.makeText(context, "Camera permission needed to take stamp photos", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerCamera = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val tempUri = ImageFileHelper.createCameraTempUri(context)
            viewModel.tempCameraUri = tempUri
            takePictureLauncher.launch(tempUri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val triggerGallery = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val startStampCreation = {
        viewModel.resetCreationForm()
        currentScreen = AppScreen.Create
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is AppScreen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            allStamps = allStamps,
                            onStampClick = { stamp -> viewModel.selectDetailStamp(stamp) },
                            onDayClick = { day ->
                                val cal = viewModel.calendarMonth.value.clone() as Calendar
                                cal.set(Calendar.DAY_OF_MONTH, day)
                                viewModel.resetCreationForm()
                                viewModel.onDateChanged(cal.timeInMillis)
                                currentScreen = AppScreen.Create
                            },
                            onOpenJournalClick = { currentScreen = AppScreen.Album }
                        )
                    }

                    is AppScreen.Album -> {
                        AlbumScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.Home },
                            onStampClick = { stamp -> viewModel.selectDetailStamp(stamp) }
                        )
                    }

                    is AppScreen.Create -> {
                        CreateStampScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.Home },
                            onTakePhotoClick = triggerCamera,
                            onPickGalleryClick = triggerGallery,
                            onSavedSuccessfully = { _, _ ->
                                currentScreen = AppScreen.Home
                            }
                        )
                    }
                }
            }

            // Material 3 Expressive Floating Navigation Pill (Screenshot 1)
            if (currentScreen != AppScreen.Create) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("expressive_floating_nav_bar"),
                    shape = RoundedCornerShape(36.dp),
                    color = Color(0xFF282522), // Warm dark espresso / charcoal pill
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Home / Calendar tab with active dot
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { currentScreen = AppScreen.Home }
                                .padding(8.dp)
                                .testTag("nav_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color(0xFFFAF7F2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            // Small active dot
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (currentScreen is AppScreen.Home) Color(0xFFFAF7F2) else Color.Transparent)
                            )
                        }

                        // 2. Friends / Community icon
                        IconButton(
                            onClick = { /* Friends tab */ },
                            modifier = Modifier.testTag("nav_friends_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Friends",
                                tint = Color(0xFFD6D1CA).copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 3. Memories / Sparkles icon
                        IconButton(
                            onClick = { currentScreen = AppScreen.Album },
                            modifier = Modifier.testTag("nav_memories_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Memories",
                                tint = Color(0xFFD6D1CA).copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 4. Dedicated Large Stamp Button on the Right (Screenshot 1)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFE2E9D1), // Soft light sage/olive accent container
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { startStampCreation() }
                                .testTag("nav_create_stamp_fab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoAlbum,
                                    contentDescription = "New Stamp",
                                    tint = Color(0xFF282522),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail & Share Modal BottomSheet
    selectedDetailStamp?.let { stamp ->
        StampDetailBottomSheet(
            stamp = stamp,
            onDismiss = { viewModel.selectDetailStamp(null) },
            onFavoriteToggle = { viewModel.toggleFavorite(stamp) },
            onDelete = { viewModel.deleteStamp(stamp) }
        )
    }
}
