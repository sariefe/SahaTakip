package com.sahatakip.util

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationServiceImpl @Inject constructor() : NotificationService {
    override fun sendPrivacySafeAlert(context: Context, title: String) {
        NotificationHelper.sendPrivacySafeAlert(context, title)
    }

    override fun createNotificationChannel(context: Context) {
        NotificationHelper.createNotificationChannel(context)
    }
}
