package com.sahatakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "offline_activity_reports",
    indices = [
        androidx.room.Index(value = ["isSynced"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
@JsonClass(generateAdapter = true)
data class OfflineActivityReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val locationAddress: String = "Saha Lokasyonu",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val reportType: String = "SAHA_DEVRIYE", // "SAHA_DEVRIYE", "GÜVENLİK_KONTROL", "KAZA_İHBARI", "BAKIM_ONARIM"
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
