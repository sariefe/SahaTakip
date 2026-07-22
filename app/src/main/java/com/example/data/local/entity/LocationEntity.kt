package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
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
