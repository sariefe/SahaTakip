package com.example.domain.repository

import com.example.data.local.entity.LeaveRequestEntity
import kotlinx.coroutines.flow.Flow

interface LeaveRepository {
    val allLeaveRequests: Flow<List<LeaveRequestEntity>>
    suspend fun deleteLeaveRequest(id: Long)
    suspend fun insertLeaveRequest(leave: LeaveRequestEntity): Long
}
