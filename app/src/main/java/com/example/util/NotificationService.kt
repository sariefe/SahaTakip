package com.example.util

import android.content.Context

interface NotificationService {
    fun sendPrivacySafeAlert(context: Context, title: String)
    fun createNotificationChannel(context: Context)
}
