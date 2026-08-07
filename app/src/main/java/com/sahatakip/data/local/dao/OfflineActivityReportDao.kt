package com.sahatakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahatakip.data.local.entity.OfflineActivityReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineActivityReportDao {
    @Query("SELECT * FROM offline_activity_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<OfflineActivityReportEntity>>

    @Query("SELECT * FROM offline_activity_reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<OfflineActivityReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: OfflineActivityReportEntity): Long

    @Query("UPDATE offline_activity_reports SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM offline_activity_reports")
    suspend fun clearAll()
}
