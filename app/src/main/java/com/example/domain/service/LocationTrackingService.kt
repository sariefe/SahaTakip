package com.example.domain.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import kotlin.time.Duration.Companion.milliseconds

class LocationTrackingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: SahaRepository

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
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1001,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1001,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationTrackingService", "Failed to start foreground service: ${e.message}", e)
            // CRITICAL: If startForeground fails, we MUST stop the service immediately
            // to avoid ForegroundServiceDidNotStartInTimeException (ANR/Crash)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LocationTrackingService", "Service onCreate")
        // Promote to foreground immediately to avoid ForegroundServiceDidNotStartInTimeException
        startForegroundServiceNotification()
        
        repository = SahaRepository(applicationContext)
        startTrackingLoop()
    }

    @SuppressLint("DefaultLocale")
    private fun startTrackingLoop() {
        serviceScope.launch {
            var currentLat = 41.0082
            var currentLng = 28.9784

            while (isActive) {
                val intervalSeconds = repository.preferencesManager.updateInterval.first()
                val latDelta = ((-10..10).random()) * 0.0002
                val lngDelta = ((-10..10).random()) * 0.0002
                currentLat += latDelta
                currentLng += lngDelta

                val speed = (10..40).random().toFloat()
                val battery = (30..100).random()

                repository.recordNewLocation(
                    lat = currentLat,
                    lng = currentLng,
                    speed = speed,
                    accuracy = 4.2f,
                    batteryLevel = battery,
                    address = "Saha Bölgesi (${String.format("%.4f", currentLat)}, ${String.format("%.4f", currentLng)})"
                )

                delay((intervalSeconds * 1000L).milliseconds)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("LocationTrackingService", "Service onStartCommand")
        // Redundant call to ensure foreground status if service was already running
        startForegroundServiceNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
