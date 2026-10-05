package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyStamp
import com.example.ui.StampViewModel
import com.example.ui.components.StampCard
import com.example.ui.components.StampPerforationShape
import com.example.util.StampImageRenderer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStampScreen(
    viewModel: StampViewModel,
    onNavigateBack: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onSavedSuccessfully: (DailyStamp, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val imageUri by viewModel.imageUriState.collectAsState()
    val caption by viewModel.captionState.collectAsState()
    val dateFormatted by viewModel.dateFormattedState.collectAsState()
    val dateMillis by viewModel.dateMillisState.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val selectedShapeType by viewModel.selectedShapeType.collectAsState()
    val isPublic by viewModel.isPublicState.collectAsState()

    val currentStamp = remember(imageUri, caption, dateFormatted, selectedShapeType, isPublic) {
        DailyStamp(
            imageUri = imageUri ?: "",
            caption = caption,
            dateMillis = dateMillis,
            dateFormatted = dateFormatted,
            locationOrTag = location,
            shapeType = selectedShapeType,
            isPublic = isPublic
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (imageUri == null) {
            // Viewfinder Mode (Screenshots 3 & 5)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(innerPadding)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("camera_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Row {
                        IconButton(onClick = onPickGalleryClick) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(onClick = { /* toggle flash */ }) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Flash",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Center: Perforated Stamp Frame Silhouette
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(
                            when (selectedShapeType) {
                                "SQUARE", "CIRCLE" -> 1f
                                "CONTOUR" -> 0.85f
                                else -> 0.78f
                            }
                        )
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    val perforationShape = remember(selectedShapeType) {
                        StampPerforationShape(
                            notchRadius = 7.dp,
                            notchSpacing = 18.dp,
                            shapeType = selectedShapeType
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(width = 3.5.dp, color = Color.White, shape = perforationShape)
                            .background(Color.White.copy(alpha = 0.08f), shape = perforationShape)
                    )
                }

                // Bottom Controls: Shape Selector & Shutter Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shape Selector Pill (Screenshot 3 & 5)
                    Surface(
                        color = Color(0x66000000),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ShapeOptionButton(
                                icon = Icons.Default.Face,
                                isSelected = selectedShapeType == "CONTOUR",
                                onClick = { viewModel.setShapeType("CONTOUR") }
                            )
                            ShapeOptionButton(
                                icon = Icons.Default.CropPortrait,
                                isSelected = selectedShapeType == "PORTRAIT",
                                onClick = { viewModel.setShapeType("PORTRAIT") }
                            )
                            ShapeOptionButton(
                                icon = Icons.Default.CropSquare,
                                isSelected = selectedShapeType == "SQUARE",
                                onClick = { viewModel.setShapeType("SQUARE") }
                            )
                            ShapeOptionButton(
                                icon = Icons.Default.RadioButtonUnchecked,
                                isSelected = selectedShapeType == "CIRCLE",
                                onClick = { viewModel.setShapeType("CIRCLE") }
                            )
                        }
                    }

                    // Shutter Bar: [1x, White Shutter, Camera Switch]
                    Surface(
                        color = Color(0x66000000),
                        shape = RoundedCornerShape(36.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(84.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1x",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Large White Circular Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { onTakePhotoClick() }
                                    .testTag("camera_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFF1E1E1E), CircleShape)
                                )
                            }

                            // Switch Camera / Gallery Trigger
                            IconButton(onClick = onPickGalleryClick) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Pick Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // "Your stamp" / "Adjust stamp" Mode (Screenshot 2 & 4)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: "Your stamp", Close (X), Gallery icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your stamp",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row {
                        IconButton(onClick = onPickGalleryClick) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Change photo",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Big Perforated Stamp
                StampCard(
                    stamp = currentStamp,
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shapeType = selectedShapeType,
                    elevation = 8.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Shape Selector Pill beneath Stamp (Screenshot 4)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShapeOptionButton(
                            icon = Icons.Default.Face,
                            isSelected = selectedShapeType == "CONTOUR",
                            onClick = { viewModel.setShapeType("CONTOUR") },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        ShapeOptionButton(
                            icon = Icons.Default.CropPortrait,
                            isSelected = selectedShapeType == "PORTRAIT",
                            onClick = { viewModel.setShapeType("PORTRAIT") },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        ShapeOptionButton(
                            icon = Icons.Default.CropSquare,
                            isSelected = selectedShapeType == "SQUARE",
                            onClick = { viewModel.setShapeType("SQUARE") },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        ShapeOptionButton(
                            icon = Icons.Default.RadioButtonUnchecked,
                            isSelected = selectedShapeType == "CIRCLE",
                            onClick = { viewModel.setShapeType("CIRCLE") },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Caption Box (Screenshot 2: clean rounded text field)
                OutlinedTextField(
                    value = caption,
                    onValueChange = { viewModel.onCaptionChanged(it) },
                    placeholder = {
                        Text(
                            text = "Caption",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("caption_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date/Time on left, character counter "0/280" on right (Screenshot 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormatted,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "${caption.length}/280",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Checkbox: "Set as public" (Screenshot 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.togglePublic(!isPublic) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPublic,
                        onCheckedChange = { viewModel.togglePublic(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Set as public",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Pills: Retake & Save (Screenshot 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Retake Pill
                    OutlinedButton(
                        onClick = {
                            viewModel.imageUriState.value = null
                            onTakePhotoClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("retake_button"),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Retake",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Save Pill (Screenshot 2: solid dark pill)
                    Button(
                        onClick = {
                            viewModel.saveNewStamp { savedStamp ->
                                onSavedSuccessfully(savedStamp, false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_stamp_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save",
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Social Media Share Button
                OutlinedButton(
                    onClick = {
                        viewModel.saveNewStamp { savedStamp ->
                            StampImageRenderer.shareStamp(context, savedStamp)
                            onSavedSuccessfully(savedStamp, true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("direct_share_button"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save & Share to Social Media",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ShapeOptionButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Surface(
        color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
