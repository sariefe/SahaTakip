package com.sahatakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "locations",
    indices = [
        androidx.room.Index(value = ["isSynced"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
@JsonClass(generateAdapter = true)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val batteryLevel: Int = 100,
    val address: String = "Saha Konumu",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
