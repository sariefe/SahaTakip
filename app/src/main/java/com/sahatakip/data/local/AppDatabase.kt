package com.sahatakip.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sahatakip.data.local.dao.EventLogDao
import com.sahatakip.data.local.dao.GeofenceDao
import com.sahatakip.data.local.dao.LeaveRequestDao
import com.sahatakip.data.local.dao.LocationDao
import com.sahatakip.data.local.dao.OfflineActivityReportDao
import com.sahatakip.data.local.dao.UserDao
import com.sahatakip.data.local.entity.EventLogEntity
import com.sahatakip.data.local.entity.GeofenceZoneEntity
import com.sahatakip.data.local.entity.LeaveRequestEntity
import com.sahatakip.data.local.entity.LocationEntity
import com.sahatakip.data.local.entity.OfflineActivityReportEntity
import com.sahatakip.data.local.entity.UserProfileEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        LocationEntity::class,
        EventLogEntity::class,
        LeaveRequestEntity::class,
        GeofenceZoneEntity::class,
        UserProfileEntity::class,
        OfflineActivityReportEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun leaveRequestDao(): LeaveRequestDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun userDao(): UserDao
    abstract fun offlineActivityReportDao(): OfflineActivityReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                System.loadLibrary("sqlcipher")

                val passphrase = "SAHA2026".toByteArray()
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saha_takip_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
