package com.example.domain.repository

import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userProfile: Flow<UserProfileEntity?>
    suspend fun initializeAndSyncDefaultData()
    suspend fun deactivateUser()
    suspend fun insertOrUpdateUser(user: UserProfileEntity)
    suspend fun updateLastLogin()
}
