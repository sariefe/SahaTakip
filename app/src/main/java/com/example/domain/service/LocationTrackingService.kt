package com.example.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.SahaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: SahaRepository

    override fun onCreate() {
        super.onCreate()
        repository = SahaRepository(applicationContext)
        startForegroundServiceNotification()
        startTrackingLoop()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "saha_tracking_service"
        val channelName = "Saha Personeli Konum Takip Servisi"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda periyodik konum kaydı yapılıyor."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Saha Takip Aktif")
            .setContentText("Saha personeli konum takibi ve güvenlik kontrolü çalışıyor.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasLocationPermission) {
                    startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(1001, notification)
                }
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(1001, notification)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun startTrackingLoop() {
        serviceScope.launch {
            var currentLat = 41.0082
            var currentLng = 28.9784

            while (isActive) {
                val intervalSeconds = repository.preferencesManager.updateInterval.first()
                // Simulate slight movement for testing
                val latDelta = ((-10..10).random()) * 0.0003
                val lngDelta = ((-10..10).random()) * 0.0003
                currentLat += latDelta
                currentLng += lngDelta

                val speed = (10..50).random().toFloat()
                val battery = (60..100).random()

                repository.recordNewLocation(
                    lat = currentLat,
                    lng = currentLng,
                    speed = speed,
                    accuracy = 4.2f,
                    batteryLevel = battery,
                    address = "Saha Bölgesi (${String.format("%.4f", currentLat)}, ${String.format("%.4f", currentLng)})"
                )

                delay(intervalSeconds * 1000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
