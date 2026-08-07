package com.sahatakip.domain.repository

import com.sahatakip.data.local.entity.LeaveRequestEntity
import kotlinx.coroutines.flow.Flow

interface LeaveRepository {
    val allLeaveRequests: Flow<List<LeaveRequestEntity>>
    suspend fun deleteLeaveRequest(id: Long)
    suspend fun insertLeaveRequest(leave: LeaveRequestEntity): Long
}
