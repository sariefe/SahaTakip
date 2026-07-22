package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.EventLogDao
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.dao.LeaveRequestDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.UserProfileEntity

@Database(
    entities = [
        LocationEntity::class,
        EventLogEntity::class,
        LeaveRequestEntity::class,
        GeofenceZoneEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun leaveRequestDao(): LeaveRequestDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saha_takip_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
