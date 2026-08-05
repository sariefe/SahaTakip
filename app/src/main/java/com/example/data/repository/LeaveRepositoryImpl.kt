package com.example.data.repository

import com.example.data.local.dao.LeaveRequestDao
import com.example.data.local.entity.LeaveRequestEntity
import com.example.domain.repository.LeaveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepositoryImpl @Inject constructor(
    private val leaveRequestDao: LeaveRequestDao,
) : LeaveRepository {

    override val allLeaveRequests: Flow<List<LeaveRequestEntity>> = leaveRequestDao.getAllLeaveRequests()

    override suspend fun deleteLeaveRequest(id: Long) = withContext(Dispatchers.IO) {
        leaveRequestDao.deleteById(id)
    }

    override suspend fun insertLeaveRequest(leave: LeaveRequestEntity) = withContext(Dispatchers.IO) {
        leaveRequestDao.insertLeaveRequest(leave)
    }
}
