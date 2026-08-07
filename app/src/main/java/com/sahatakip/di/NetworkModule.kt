package com.sahatakip.di

import com.sahatakip.data.remote.MockSyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMockSyncApi(): MockSyncApi {
        return MockSyncApi()
    }
}
