package com.sahatakip.di

import android.content.Context
import com.sahatakip.data.local.AppDatabase
import com.sahatakip.data.local.dao.EventLogDao
import com.sahatakip.data.local.dao.GeofenceDao
import com.sahatakip.data.local.dao.LeaveRequestDao
import com.sahatakip.data.local.dao.LocationDao
import com.sahatakip.data.local.dao.OfflineActivityReportDao
import com.sahatakip.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideEventLogDao(database: AppDatabase): EventLogDao = database.eventLogDao()

    @Provides
    fun provideLeaveRequestDao(database: AppDatabase): LeaveRequestDao = database.leaveRequestDao()

    @Provides
    fun provideGeofenceDao(database: AppDatabase): GeofenceDao = database.geofenceDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideOfflineReportDao(database: AppDatabase): OfflineActivityReportDao = database.offlineActivityReportDao()
}
