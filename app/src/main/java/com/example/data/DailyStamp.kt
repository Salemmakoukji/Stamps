package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stamps")
data class DailyStamp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val caption: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val dateFormatted: String,
    val locationOrTag: String = "",
    val stampThemeColor: String = "#F9F6F0", // Background color of stamp
    val stampBorderColor: String = "#E29578", // Accent border color
    val stampStyle: String = "CLASSIC",      // CLASSIC, AIRMAIL, ROYAL, MINIMAL
    val postmarkStyle: String = "WAVE_SEAL", // WAVE_SEAL, DOUBLE_RING, AIR_MAIL, STAR
    val denomination: String = "0.50",       // Denomination text e.g. "0.50" or "١.٠٠"
    val postmarkLocation: String = "البريد اليومي", // Postmark city/location
    val shapeType: String = "PORTRAIT",      // PORTRAIT, SQUARE, CIRCLE, CONTOUR
    val isPublic: Boolean = false,
    val isFavorite: Boolean = false
)
