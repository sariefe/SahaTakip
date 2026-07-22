package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_zones")
data class GeofenceZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Double = 500.0,
    val isActive: Boolean = true
)
