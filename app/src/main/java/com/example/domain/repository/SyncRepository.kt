package com.example.domain.repository

interface SyncRepository {
    suspend fun performOfflineSync(): Boolean
}
