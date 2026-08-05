package com.example.data.repository

import com.example.data.local.PreferencesManager
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.repository.EventRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val eventRepository: EventRepository,
) : UserRepository {

    override val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    override suspend fun initializeAndSyncDefaultData() = withContext(Dispatchers.IO) {
        val currentUser = userDao.getUserProfile().firstOrNull()
        if (currentUser == null) {
            userDao.insertOrUpdateUser(
                UserProfileEntity(
                    id = 1,
                    firstName = "Ömer",
                    lastName = "Saha",
                    fullName = "Ömer Saha",
                    position = "Saha Teknisyeni",
                    department = "SAHA",
                    staffId = "ID-2026-DEMO",
                    roleTitle = "Saha Personeli (Demo)",
                    activationCode = PreferencesManager.DEFAULT_ACTIVATION_CODE,
                    isActivated = false,
                    isBiometricEnabled = true,
                    isCheckedIn = false,
                )
            )
        }
    }

    override suspend fun deactivateUser() = withContext(Dispatchers.IO) {
        userDao.deactivateUser()
        eventRepository.addEventLog(
            type = "APP_DEACTIVATED",
            title = "Cihaz Devre Dışı",
            detail = "Personel uygulama oturumunu tamamen sonlandırdı ve cihazı deaktive etti.",
            status = "UYARI"
        )
    }

    override suspend fun insertOrUpdateUser(user: UserProfileEntity) = withContext(Dispatchers.IO) {
        userDao.insertOrUpdateUser(user)
    }

    override suspend fun updateLastLogin() = withContext(Dispatchers.IO) {
        userDao.updateLastLogin()
    }
}
