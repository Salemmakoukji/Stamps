package com.example.data.firebase

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalStamps: Int = 0,
    val lastActiveDate: String = "",
    val updatedAt: Timestamp? = null
)
