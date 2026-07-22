package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    private const val CHANNEL_ID = "saha_security_alerts"
    private const val CHANNEL_NAME = "Saha Güvenlik ve Bölge İhlal Bildirimleri"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Saha personeli güvenlik ve konum durumu genel uyarıları"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Requirement 6: Sensitive details MUST NOT be exposed in plaintext inside notifications!
     * Only general alert text is shown; details are viewed inside the app.
     */
    fun sendPrivacySafeAlert(context: Context, alertTitle: String = "Saha Takip Uyarısı") {
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

        // Privacy safe alert text: generic message without raw GPS / violation details
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alertTitle)
            .setContentText("Saha uygulamanızda yeni bir güvenlik/durum olayı kaydedildi. Detaylar için dokunun.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Saha personeli takip sisteminde yeni bir durum değişikliği oluştu. Gizlilik gereği detaylar bildirim çubuğunda gösterilmez. Detayları görmek için uygulamayı açınız."
                )
            )
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
