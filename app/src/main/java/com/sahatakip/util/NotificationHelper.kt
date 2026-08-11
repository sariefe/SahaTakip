package com.sahatakip.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sahatakip.MainActivity
import com.sahatakip.R
import com.sahatakip.data.local.PreferencesManager

object NotificationHelper {

    private const val CHANNEL_ID = "saha_security_alerts"
    private var lastNotificationTime = 0L
    private const val NOTIFICATION_COOLDOWN_MS = 30_000L

    fun createNotificationChannel(context: Context) {
        val prefs = PreferencesManager(context)
        val lang = prefs.language.value
        val channelName = trGlobal("Saha Güvenlik ve Bölge İhlal Bildirimleri", "Field Security and Geofence Alerts", lang)

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
            description = trGlobal("Saha personeli güvenlik ve konum durumu genel uyarıları", "General alerts for field personnel security and location status", lang)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Requirement 6: Sensitive details MUST NOT be exposed in plaintext inside notifications!
     * Only general alert text is shown; details are viewed inside the app.
     */
    fun sendPrivacySafeAlert(context: Context, alertTitle: String = "Saha Takip Uyarısı") {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < NOTIFICATION_COOLDOWN_MS) {
            android.util.Log.d("NotificationHelper", "Notification suppressed due to rate limiting.")
            return
        }
        lastNotificationTime = now

        val prefs = PreferencesManager(context)
        val lang = prefs.language.value

        val finalTitle = when (alertTitle) {
            "Saha Takip Uyarısı" -> {
                trGlobal("Saha Takip Uyarısı", "Field Tracking Alert", lang)
            }
            "Güvenlik & Bölge İhlali Uyarısı" -> {
                trGlobal("Güvenlik & Bölge İhlali Uyarısı", "Security & Geofence Alert", lang)
            }
            else -> {
                alertTitle
            }
        }

        val contentShort = trGlobal("Saha uygulamanızda yeni bir güvenlik/durum olayı kaydedildi. Detaylar için dokunun.", "A new security/status event has been recorded. Tap for details.", lang)
        val contentLong = trGlobal(
            "Saha personeli takip sisteminde yeni bir durum değişikliği oluştu. Gizlilik gereği detaylar bildirim çubuğunda gösterilmez. Detayları görmek için uygulamayı açınız.",
            "A new status change occurred in the field tracking system. For privacy, details are not shown in the notification bar. Open the app to see details.",
            lang
        )

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_stat)
            .setContentTitle(finalTitle)
            .setContentText(contentShort)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentLong))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
