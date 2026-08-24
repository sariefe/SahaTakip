package com.sahatakip.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sahatakip.domain.service.LocationTrackingService
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.tag("BootReceiver").d("Device boot completed. Checking permissions.")
            
            val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasFineLocation || hasCoarseLocation) {
                Timber.tag("BootReceiver").d("Starting LocationTrackingService.")
                val serviceIntent = Intent(context, LocationTrackingService::class.java)
                try {
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    Timber.tag("BootReceiver").e(e, "Failed to start service on boot")
                }
            } else {
                Timber.tag("BootReceiver").w("Location permissions missing. Service not started.")
            }
        }
    }
}
