package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.domain.service.LocationTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            android.util.Log.d("BootReceiver", "Device boot completed. Starting LocationTrackingService.")
            val serviceIntent = Intent(context, LocationTrackingService::class.java)
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to start service on boot: ${e.message}")
            }
        }
    }
}
