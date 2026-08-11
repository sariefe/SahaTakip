package com.sahatakip.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sahatakip.domain.service.LocationTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            android.util.Log.d("BootReceiver", "Device boot completed. Checking permissions.")
            
            val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasFineLocation || hasCoarseLocation) {
                android.util.Log.d("BootReceiver", "Starting LocationTrackingService.")
                val serviceIntent = Intent(context, LocationTrackingService::class.java)
                try {
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "Failed to start service on boot: ${e.message}")
                }
            } else {
                android.util.Log.w("BootReceiver", "Location permissions missing. Service not started.")
            }
        }
    }
}
