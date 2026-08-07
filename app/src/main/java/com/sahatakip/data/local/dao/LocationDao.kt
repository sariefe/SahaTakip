package com.sahatakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahatakip.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getLocationsSince(sinceTimestamp: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isSynced = 0")
    suspend fun getUnsyncedLocations(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Query("UPDATE locations SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM locations")
    suspend fun clearAll()
}
