package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,
    val endDate: String,
    val requestType: String, // "Yıllık İzin", "Mazeret İzni", "Sağlık İzni", "Görevli İzin"
    val reason: String,
    val status: String = "BEKLEMEDE", // "BEKLEMEDE", "ONAYLANDI", "REDDEDİLDİ"
    val submittedAt: Long = System.currentTimeMillis()
)
