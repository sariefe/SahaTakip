package com.sahatakip.data.repository

import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.data.local.dao.UserDao
import com.sahatakip.data.local.entity.UserProfileEntity
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.GeofenceRepository
import com.sahatakip.domain.repository.UserRepository
import com.sahatakip.data.local.entity.GeofenceZoneEntity
import com.sahatakip.util.Constants
import com.sahatakip.util.trGlobal
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
    private val geofenceRepository: GeofenceRepository,
    private val preferencesManager: PreferencesManager,
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

        val currentGeofences = geofenceRepository.getAllGeofencesOnce()
        if (currentGeofences.isEmpty()) {
            val lang = preferencesManager.language.value
            geofenceRepository.insertGeofence(
                GeofenceZoneEntity(
                    name = trGlobal("Merkez Şantiye Alanı", "Central Construction Site", lang),
                    centerLat = 41.0125,
                    centerLng = 28.9810,
                    radiusMeters = 1000.0,
                    isActive = true
                )
            )
        }
    }

    override suspend fun deactivateUser() = withContext(Dispatchers.IO) {
        userDao.deactivateUser()
        val lang = preferencesManager.language.value
        eventRepository.addEventLog(
            type = "APP_DEACTIVATED",
            title = trGlobal("Cihaz Devre Dışı", "Device Deactivated", lang),
            detail = trGlobal(
                "Personel uygulama oturumunu tamamen sonlandırdı ve cihazı deaktive etti.",
                "Personnel completely terminated the app session and deactivated the device.",
                lang
            ),
            status = Constants.STATUS_WARNING
        )
    }

    override suspend fun insertOrUpdateUser(user: UserProfileEntity) = withContext(Dispatchers.IO) {
        userDao.insertOrUpdateUser(user)
    }

    override suspend fun updateLastLogin() = withContext(Dispatchers.IO) {
        userDao.updateLastLogin()
    }
}
