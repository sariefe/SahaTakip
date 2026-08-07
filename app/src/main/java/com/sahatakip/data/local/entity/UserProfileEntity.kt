package com.sahatakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "user_profile")
@JsonClass(generateAdapter = true)
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val position: String = "Saha Personeli",
    val department: String = "Saha",
    val staffId: String = "",
    val roleTitle: String = "Saha Saha Personeli",
    val activationCode: String = "",
    val isActivated: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val isCheckedIn: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
