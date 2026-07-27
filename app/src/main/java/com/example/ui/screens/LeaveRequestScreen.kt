package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Data model for local state storage
data class LocalLeaveRequestItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val description: String,
    val status: String,
    val type: String = "Mazeret İzni"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    viewModel: MainViewModel
) {
    val dbRequests by viewModel.allLeaveRequests.collectAsState()

    // Local state array holding leave requests / excuses as requested
    val localLeaveList = remember {
        mutableStateListOf(
            LocalLeaveRequestItem(
                date = "22.07.2026",
                description = "Yıllık periyodik sağlık kontrolü randevusu.",
                status = "ONAYLANDI",
                type = "Sağlık İzni"
            ),
            LocalLeaveRequestItem(
                date = "25.07.2026",
                description = "Ailevi mazeret nedeni ile saha izin talebi.",
                status = "BEKLEMEDE",
                type = "Mazeret İzni"
            )
        )
    }

    var showForm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("İzin & Mazeret Formu", "Leave & Excuse Form"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Saha personel izin talepleri ve mazeret bildirimleri", "Field personnel leave requests and excuse notices"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showForm = !showForm },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showForm) tr("Kapat", "Close") else tr("Yeni Talep", "New Request"))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Component
            AnimatedVisibility(
                visible = showForm,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LeaveSubmitFormComponent(
                    onSubmit = { newDate, newDesc, newStatus, newType ->
                        // Store in local state array
                        localLeaveList.add(
                            0,
                            LocalLeaveRequestItem(
                                date = newDate,
                                description = newDesc,
                                status = newStatus,
                                type = newType
                            )
                        )
                        // Also persist via Room DB repository
                        viewModel.submitLeaveRequest(
                            type = newType,
                            startDate = newDate,
                            endDate = newDate,
                            reason = newDesc
                        )
                        showForm = false
                    },
                    onCancel = { showForm = false }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Combine local items and room DB items for display
            val combinedList = remember(localLeaveList.size, dbRequests.size) {
                val dbMapped = dbRequests.map { req ->
                    LocalLeaveRequestItem(
                        id = req.id.toString(),
                        date = req.startDate,
                        description = req.reason,
                        status = req.status,
                        type = req.requestType
                    )
                }
                (localLeaveList + dbMapped).distinctBy { it.id }
            }

            Text(
                text = "${tr("Kayıtlı İzin & Mazeret Talepleri", "Saved Leave & Excuse Requests")} (${combinedList.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (combinedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr("Henüz kayıtlı izin veya mazeret talebi bulunmamaktadır.", "No saved leave or excuse requests found."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(combinedList, key = { it.id }) { item ->
                        LeaveRequestCardItem(
                            item = item,
                            onDelete = {
                                localLeaveList.removeIf { local -> local.id == item.id }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveSubmitFormComponent(
    onSubmit: (date: String, description: String, status: String, type: String) -> Unit,
    onCancel: () -> Unit
) {
    val currentDateStr = remember {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        formatter.format(Date())
    }

    var dateInput by remember { mutableStateOf(currentDateStr) }
    var descriptionInput by remember { mutableStateOf("") }

    val statusOptions = listOf("BEKLEMEDE", "ONAYLANDI", "REDDEDİLDİ")
    var selectedStatus by remember { mutableStateOf(statusOptions[0]) }
    var expandedStatusDropdown by remember { mutableStateOf(false) }

    val leaveTypes = listOf(
        tr("Mazeret İzni", "Excuse Leave"),
        tr("Yıllık İzin", "Annual Leave"),
        tr("Sağlık İzni", "Sick Leave"),
        tr("Görevli İzin", "Duty Leave")
    )
    var selectedType by remember { mutableStateOf(leaveTypes[0]) }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tr("Talep Formu Doldur", "Fill Request Form"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row: Type & Status Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Talep Türü", "Request Type")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        leaveTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedStatusDropdown,
                    onExpandedChange = { expandedStatusDropdown = !expandedStatusDropdown },
                    modifier = Modifier.weight(1f)
                ) {
                    val statusLabel = when (selectedStatus) {
                        "ONAYLANDI" -> tr("ONAYLANDI", "APPROVED")
                        "REDDEDİLDİ" -> tr("REDDEDİLDİ", "REJECTED")
                        else -> tr("BEKLEMEDE", "PENDING")
                    }

                    OutlinedTextField(
                        value = statusLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Durum (Status)", "Status")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatusDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStatusDropdown,
                        onDismissRequest = { expandedStatusDropdown = false }
                    ) {
                        statusOptions.forEach { st ->
                            val stDisplay = when (st) {
                                "ONAYLANDI" -> tr("ONAYLANDI", "APPROVED")
                                "REDDEDİLDİ" -> tr("REDDEDİLDİ", "REJECTED")
                                else -> tr("BEKLEMEDE", "PENDING")
                            }
                            DropdownMenuItem(
                                text = { Text(stDisplay) },
                                onClick = {
                                    selectedStatus = st
                                    expandedStatusDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Field
            OutlinedTextField(
                value = dateInput,
                onValueChange = {
                    dateInput = it
                    errorMessage = null
                },
                label = { Text(tr("Tarih (Date)", "Date")) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description / Reason Field
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = {
                    descriptionInput = it
                    errorMessage = null
                },
                label = { Text(tr("Açıklama / Mazeret", "Description / Excuse Reason")) },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                placeholder = { Text(tr("Lütfen izin veya mazeret nedenini detaylandırın...", "Please elaborate on reason for leave or excuse...")) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = err,
                    color = StatusRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val emptyDescErr = tr("Lütfen geçerli bir mazeret açıklaması giriniz.", "Please enter a valid excuse description.")
            val emptyDateErr = tr("Lütfen tarih alanını boş bırakmayınız.", "Please do not leave the date field empty.")

            // Actions: Submit & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(tr("İptal", "Cancel"))
                }

                Button(
                    onClick = {
                        if (descriptionInput.trim().isEmpty()) {
                            errorMessage = emptyDescErr
                        } else if (dateInput.trim().isEmpty()) {
                            errorMessage = emptyDateErr
                        } else {
                            onSubmit(dateInput.trim(), descriptionInput.trim(), selectedStatus, selectedType)
                        }
                    }
                ) {
                    Text(tr("Talebi Kaydet", "Save Request"), fontWeight = FontWeight.Bold)
                }
            }

        }
    }

}

@Composable
fun LeaveRequestCardItem(
    item: LocalLeaveRequestItem,
    onDelete: () -> Unit
) {
    val statusColor = when (item.status.uppercase()) {
        "ONAYLANDI" -> StatusGreen
        "REDDEDİLDİ" -> StatusRed
        else -> StatusAmber
    }

    val statusIcon = when (item.status.uppercase()) {
        "ONAYLANDI" -> Icons.Default.CheckCircle
        "REDDEDİLDİ" -> Icons.Default.Report
        else -> Icons.Default.HourglassTop
    }

    val statusLabel = when (item.status.uppercase()) {
        "ONAYLANDI" -> tr("ONAYLANDI", "APPROVED")
        "REDDEDİLDİ" -> tr("REDDEDİLDİ", "REJECTED")
        else -> tr("BEKLEMEDE", "PENDING")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = tr("Sil", "Delete"),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${tr("Tarih", "Date")}: ${item.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${tr("Açıklama", "Description")}: ${item.description}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

}

