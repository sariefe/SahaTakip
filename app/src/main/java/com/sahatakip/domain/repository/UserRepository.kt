package com.sahatakip.domain.repository

import com.sahatakip.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userProfile: Flow<UserProfileEntity?>
    suspend fun initializeAndSyncDefaultData()
    suspend fun deactivateUser()
    suspend fun insertOrUpdateUser(user: UserProfileEntity)
    suspend fun updateLastLogin()
}
