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
import java.security.SecureRandom
import androidx.core.content.edit

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

        private const val PREFS_NAME = "database_security"
        private const val KEY_DB_PASSWORD = "db_password"

        private fun generatePassword(): String {
            val chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

            val random = SecureRandom()

            return (1..32)
                .map {
                    chars[random.nextInt(chars.length)]
                }
                .joinToString("")
        }

        private fun getDatabasePassword(context: Context): String {

            val prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            var password = prefs.getString(KEY_DB_PASSWORD, null)

            if (password == null) {
                password = generatePassword()

                prefs.edit {
                    putString(KEY_DB_PASSWORD, password)
                }
            }

            return password
        }

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                System.loadLibrary("sqlcipher")

                val password = getDatabasePassword(
                    context.applicationContext
                )

                val factory = SupportOpenHelperFactory(
                    password.toByteArray()
                )

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saha_takip_database"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration(
                        dropAllTables = true
                    )
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}