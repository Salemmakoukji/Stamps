package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DailyStamp

@Composable
fun StampCard(
    stamp: DailyStamp,
    modifier: Modifier = Modifier,
    shapeType: String = stamp.shapeType,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null
) {
    val aspectRatio = when (shapeType) {
        "SQUARE", "CIRCLE" -> 1f
        "CONTOUR" -> 0.85f
        else -> 0.78f // Standard portrait stamp
    }

    val perforationShape = remember(shapeType) {
        StampPerforationShape(
            notchRadius = 6.dp,
            notchSpacing = 16.dp,
            shapeType = shapeType
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .shadow(elevation = elevation, shape = perforationShape)
            .clip(perforationShape)
            .background(Color.White)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(width = 3.dp, color = Color.White, shape = perforationShape)
            .padding(2.dp)
            .testTag("stamp_card_${stamp.id}")
    ) {
        // Photo inside the stamp
        AsyncImage(
            model = stamp.imageUri,
            contentDescription = stamp.caption.ifBlank { "Daily stamp" },
            modifier = Modifier
                .fillMaxSize()
                .clip(perforationShape),
            contentScale = ContentScale.Crop
        )

        // Subtle perforated inner rim highlight for tactile depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f), shape = perforationShape)
        )
    }
}

// Mini stamp for the monthly calendar grid cells (Screenshot 1)
@Composable
fun MiniStampCard(
    stamp: DailyStamp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val perforationShape = remember(stamp.shapeType) {
        StampPerforationShape(
            notchRadius = 2.5.dp,
            notchSpacing = 7.dp,
            shapeType = stamp.shapeType
        )
    }

    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = perforationShape)
            .clip(perforationShape)
            .background(Color.White)
            .border(width = 1.5.dp, color = Color.White, shape = perforationShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = stamp.imageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(perforationShape),
            contentScale = ContentScale.Crop
        )
    }
}
