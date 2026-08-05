package com.example.di

import com.example.data.repository.EventRepositoryImpl
import com.example.data.repository.GeofenceRepositoryImpl
import com.example.data.repository.LeaveRepositoryImpl
import com.example.data.repository.LocationRepositoryImpl
import com.example.data.repository.SyncRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.repository.EventRepository
import com.example.domain.repository.GeofenceRepository
import com.example.domain.repository.LeaveRepository
import com.example.domain.repository.LocationRepository
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
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
