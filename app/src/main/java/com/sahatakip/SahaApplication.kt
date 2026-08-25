package com.sahatakip

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sahatakip.domain.worker.LocationWatchdogWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SahaApplication : Application(), Configuration.Provider {
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        //build olduğunda loglar görünmesin
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setupLocationWatchdog()
    }

    private fun setupLocationWatchdog() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val watchdogRequest = PeriodicWorkRequestBuilder<LocationWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LocationTrackingWatchdog",
                ExistingPeriodicWorkPolicy.KEEP,
                watchdogRequest
            )
            Timber.tag("SahaApplication").d("Location watchdog scheduled.")
        } catch (e: Exception) {
            Timber.tag("SahaApplication").e(e, "Failed to setup location watchdog")
        }
    }
}
