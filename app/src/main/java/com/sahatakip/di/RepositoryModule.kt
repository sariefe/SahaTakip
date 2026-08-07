package com.sahatakip.di

import com.sahatakip.data.repository.EventRepositoryImpl
import com.sahatakip.data.repository.GeofenceRepositoryImpl
import com.sahatakip.data.repository.LeaveRepositoryImpl
import com.sahatakip.data.repository.LocationRepositoryImpl
import com.sahatakip.data.repository.SyncRepositoryImpl
import com.sahatakip.data.repository.UserRepositoryImpl
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.GeofenceRepository
import com.sahatakip.domain.repository.LeaveRepository
import com.sahatakip.domain.repository.LocationRepository
import com.sahatakip.domain.repository.SyncRepository
import com.sahatakip.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindGeofenceRepository(
        geofenceRepositoryImpl: GeofenceRepositoryImpl
    ): GeofenceRepository

    @Binds
    @Singleton
    abstract fun bindLeaveRepository(
        leaveRepositoryImpl: LeaveRepositoryImpl
    ): LeaveRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository
}
