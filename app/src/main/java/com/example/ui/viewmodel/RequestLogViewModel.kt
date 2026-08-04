package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.EventLogEntity
import com.example.data.local.entity.LeaveRequestEntity
import com.example.data.repository.SahaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestLogViewModel @Inject constructor(
    private val repository: SahaRepository
) : androidx.lifecycle.ViewModel() {

    val allEventLogs: StateFlow<List<EventLogEntity>> = repository.allEventLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLeaveRequests: StateFlow<List<LeaveRequestEntity>> = repository.allLeaveRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addNoteToEventLog(logId: Long, note: String) {
        viewModelScope.launch {
            repository.eventLogDao.updateNote(logId, note)
        }
    }

    fun submitLeaveRequest(type: String, startDate: String, endDate: String, reason: String) {
        viewModelScope.launch {
            repository.leaveRequestDao.insertLeaveRequest(
                LeaveRequestEntity(
                    startDate = startDate,
                    endDate = endDate,
                    requestType = type,
                    reason = reason,
                    status = "BEKLEMEDE"
                )
            )
        }
    }

    fun deleteLeaveRequest(id: Long) {
        viewModelScope.launch {
            repository.deleteLeaveRequest(id)
        }
    }
}
