package com.sahatakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahatakip.data.local.entity.GeofenceZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_zones ORDER BY id ASC")
    fun getAllGeofences(): Flow<List<GeofenceZoneEntity>>

    @Query("SELECT * FROM geofence_zones WHERE isActive = 1")
    suspend fun getActiveGeofences(): List<GeofenceZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceZoneEntity): Long

    @Query("UPDATE geofence_zones SET isActive = :isActive WHERE id = :id")
    suspend fun setGeofenceActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM geofence_zones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
