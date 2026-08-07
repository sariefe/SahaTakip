package com.sahatakip.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.sahatakip.data.local.entity.EventLogEntity
import com.sahatakip.data.local.entity.LeaveRequestEntity
import com.sahatakip.domain.repository.EventRepository
import com.sahatakip.domain.repository.LeaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestLogViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val leaveRepository: LeaveRepository,
) : androidx.lifecycle.ViewModel() {

    val allEventLogs: StateFlow<List<EventLogEntity>> = eventRepository.allEventLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLeaveRequests: StateFlow<List<LeaveRequestEntity>> = leaveRepository.allLeaveRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addNoteToEventLog(logId: Long, note: String) {
        viewModelScope.launch {
            eventRepository.updateNote(logId, note)
        }
    }

    fun clearAllEventLogs() {
        viewModelScope.launch {
            eventRepository.clearAllLogs()
        }
    }

    fun submitLeaveRequest(type: String, startDate: String, endDate: String, reason: String) {
        viewModelScope.launch {
            leaveRepository.insertLeaveRequest(
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
            leaveRepository.deleteLeaveRequest(id)
        }
    }
}
