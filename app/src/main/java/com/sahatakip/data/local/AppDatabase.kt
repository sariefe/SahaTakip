package com.sahatakip.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dbKeyStore: DataStore<Preferences>
        by preferencesDataStore(name = "db_key_store")

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
        private val PREF_ENCRYPTED_PASS = stringPreferencesKey("enc_db_pass_v1")


        private const val KEYSTORE_ALIAS= "sahatakip_db_key"
        private const val KEYSTORE_PROVIDER= "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION= "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH=128
        private const val DB_PASSWORD_BYTES= 32

        private fun getOrCreateKeystoreKey(): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            return KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .apply { init(spec) }
                .generateKey()
        }

        private fun encrypt(plaintext: ByteArray): String {
            val key = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key)
            }
            val combined = cipher.iv + cipher.doFinal(plaintext)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        private fun decrypt(encryptedBase64: String): ByteArray {
            val key = getOrCreateKeystoreKey()
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            }
            return cipher.doFinal(ciphertext)
        }
        private fun getDatabasePassword(context: Context): String {
            return runBlocking {
                val prefs = context.applicationContext.dbKeyStore.data.first()
                val stored = prefs[PREF_ENCRYPTED_PASS]

                if (stored != null) {
                    decrypt(stored).joinToString("") { "%02x".format(it) }
                } else {
                    val rawPassword = ByteArray(DB_PASSWORD_BYTES)
                        .also { SecureRandom().nextBytes(it) }

                    context.applicationContext.dbKeyStore.edit { ds ->
                        ds[PREF_ENCRYPTED_PASS] = encrypt(rawPassword)
                    }

                    rawPassword.joinToString("") { "%02x".format(it) }
                }
            }
        }
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }

                val isTest = try {
                    Class.forName("org.robolectric.Robolectric")
                    true
                } catch (_: Exception) {
                    false
                }

                if (!isTest) {
                    try {
                        System.loadLibrary("sqlcipher")
                    } catch (_: UnsatisfiedLinkError) {
                    }
                }

                context.applicationContext.deleteDatabase("saha_takip_database")
                
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "saha_takip_database"
                )

                if (!isTest) {
                    val password = getDatabasePassword(context.applicationContext)
                    val factory = SupportOpenHelperFactory(password.toByteArray(Charsets.UTF_8))
                    builder.openHelperFactory(factory)
                }

                builder.fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}