package com.sahatakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "geofence_zones")
@JsonClass(generateAdapter = true)
data class GeofenceZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Double = 500.0,
    val isActive: Boolean = true
)
