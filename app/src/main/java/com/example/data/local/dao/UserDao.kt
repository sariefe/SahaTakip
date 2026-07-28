package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserProfileEntity)

    @Query("UPDATE user_profile SET lastLoginAt = :timestamp WHERE id = 1")
    suspend fun updateLastLogin(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET isBiometricEnabled = :enabled WHERE id = 1")
    suspend fun updateBiometricPreference(enabled: Boolean)

    @Query("UPDATE user_profile SET isActivated = 0 WHERE id = 1")
    suspend fun deactivateUser()
}
