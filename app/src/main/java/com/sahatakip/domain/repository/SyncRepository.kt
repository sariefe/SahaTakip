package com.sahatakip.domain.repository

interface SyncRepository {
    suspend fun performOfflineSync(): Boolean
}
