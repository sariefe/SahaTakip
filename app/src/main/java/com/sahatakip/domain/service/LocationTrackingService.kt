package com.sahatakip.domain.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.sahatakip.MainActivity
import com.sahatakip.data.local.PreferencesManager
import com.sahatakip.domain.repository.LocationRepository
import com.sahatakip.util.LocationUtils
import com.sahatakip.util.trGlobal
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sahatakip.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val addressMutex = Mutex()
    
    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private fun startForegroundServiceNotification() {
        val lang = preferencesManager.language.value
        val channelId = "saha_tracking_service"
        val channelName = trGlobal("Saha Personeli Konum Takip Servisi", "Field Staff Location Tracking Service", lang)

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = trGlobal("Arka planda periyodik konum kaydı yapılıyor.", "Periodic location recording in the background.", lang)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(trGlobal("Saha Takip Aktif", "Field Tracking Active", lang))
            .setContentText(trGlobal("Saha personeli konum takibi ve güvenlik kontrolü çalışıyor.", "Field staff location tracking and security check is running.", lang))
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                android.util.Log.e("LocationTrackingService", "Starting service without location permissions. Stopping.")
                stopSelf()
                return
            }

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
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && (e is android.app.ForegroundServiceStartNotAllowedException)) {
                android.util.Log.e("LocationTrackingService", "FGS start not allowed from background", e)
            } else {
                android.util.Log.e("LocationTrackingService", "Failed to start foreground service: ${e.message}", e)
            }
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LocationTrackingService", "Service onCreate")
        startForegroundServiceNotification()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupLocationCallback()
        startLocationUpdates()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    saveLocationToRepository(location)
                }
            }
        }
    }

    private fun saveLocationToRepository(location: Location) {
        if (location.latitude == 0.0 && location.longitude == 0.0) return
        
        serviceScope.launch {
            val address = addressMutex.withLock {
                LocationUtils.getAddressFromLocation(
                    applicationContext,
                    location.latitude,
                    location.longitude
                )
            }
            val batteryStatus = getBatteryLevel()
            locationRepository.recordNewLocation(
                lat = location.latitude,
                lng = location.longitude,
                speed = location.speed,
                accuracy = location.accuracy,
                batteryLevel = batteryStatus,
                address = address
            )
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        serviceScope.launch {
            preferencesManager.updateInterval.collectLatest { intervalSeconds ->
                val batteryLevel = getBatteryLevel()
                val priority = if (batteryLevel < 20) {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                } else {
                    Priority.PRIORITY_HIGH_ACCURACY
                }

                val locationRequest = LocationRequest.Builder(priority, intervalSeconds * 1000L)
                    .setMinUpdateIntervalMillis(intervalSeconds * 500L)
                    .build()

                fusedLocationClient.removeLocationUpdates(locationCallback)
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                android.util.Log.d("LocationTrackingService", "Location updates restarted. Priority: $priority, Interval: $intervalSeconds s")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("LocationTrackingService", "Service onStartCommand")
        startForegroundServiceNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
