package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val tcNo: String = "",
    val roleTitle: String = "Saha Saha Personeli",
    val activationCode: String = "",
    val isActivated: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val registeredAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
