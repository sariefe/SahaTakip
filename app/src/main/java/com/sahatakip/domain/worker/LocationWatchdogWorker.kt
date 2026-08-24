package com.sahatakip.domain.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sahatakip.domain.service.LocationTrackingService
import timber.log.Timber

class LocationWatchdogWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Timber.tag("LocationWatchdogWorker").d("Watchdog checking service status...")
        
        val serviceIntent = Intent(applicationContext, LocationTrackingService::class.java)
        try {
            applicationContext.startForegroundService(serviceIntent)
            Timber.tag("LocationWatchdogWorker").d("Service start command sent by watchdog.")
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                Timber.tag("LocationWatchdogWorker").e("Foreground service start not allowed from background WorkManager")
            } else {
                Timber.tag("LocationWatchdogWorker").e(e, "Watchdog failed to start service")
            }
            return Result.retry()
        }

        return Result.success()
    }
}
