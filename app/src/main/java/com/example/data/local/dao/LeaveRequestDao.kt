package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LeaveRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveRequestDao {
    @Query("SELECT * FROM leave_requests ORDER BY submittedAt DESC")
    fun getAllLeaveRequests(): Flow<List<LeaveRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaveRequest(request: LeaveRequestEntity): Long

    @Query("UPDATE leave_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM leave_requests WHERE id = :id")
    suspend fun deleteById(id: Long)
}
