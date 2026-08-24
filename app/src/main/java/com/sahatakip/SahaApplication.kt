package com.sahatakip

import android.app.Application
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
class SahaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setupLocationWatchdog()
    }

    private fun setupLocationWatchdog() {
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
    }
}
