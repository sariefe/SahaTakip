package com.example.di

import com.example.util.NotificationService
import com.example.util.NotificationServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindNotificationService(
        notificationServiceImpl: NotificationServiceImpl
    ): NotificationService
}
