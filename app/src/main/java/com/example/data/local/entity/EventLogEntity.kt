package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "event_logs",
    indices = [
        androidx.room.Index(value = ["isSynced"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
@JsonClass(generateAdapter = true)
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "GEOFENCE_VIOLATION", "INTERNET_LOST", "INTERNET_RESTORED", "GPS_DISABLED", "GPS_ENABLED", "SECURITY_ALERT", "BATTERY_LOW", "CUSTOM"
    val title: String,
    val detail: String,
    val isSensitive: Boolean = true,
    val note: String = "",
    val status: String = "SİSTEM", // "UYARI", "BİLGİ", "TEHLİKE"
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
