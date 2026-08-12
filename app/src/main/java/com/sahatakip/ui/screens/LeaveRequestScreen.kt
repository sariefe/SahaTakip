package com.sahatakip.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahatakip.data.local.entity.LeaveRequestEntity
import com.sahatakip.ui.theme.StatusAmber
import com.sahatakip.ui.theme.StatusGreen
import com.sahatakip.ui.theme.StatusRed
import com.sahatakip.ui.viewmodel.RequestLogViewModel
import com.sahatakip.util.Constants
import com.sahatakip.util.tr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


import androidx.compose.ui.tooling.preview.Preview
import com.sahatakip.ui.theme.SahaTakipTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    viewModel: RequestLogViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    val dbRequests by viewModel.allLeaveRequests.collectAsStateWithLifecycle()

    LeaveRequestScreenContent(
        dbRequests = dbRequests,
        windowWidthSizeClass = windowWidthSizeClass,
        onSubmitRequest = { type, start, end, reason ->
            viewModel.submitLeaveRequest(type = type, startDate = start, endDate = end, reason = reason)
        },
        onDeleteRequest = { viewModel.deleteLeaveRequest(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreenContent(
    dbRequests: List<LeaveRequestEntity>,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSubmitRequest: (type: String, start: String, end: String, reason: String) -> Unit,
    onDeleteRequest: (Long) -> Unit
) {
    var showForm by remember(windowWidthSizeClass) { 
        mutableStateOf(windowWidthSizeClass != WindowWidthSizeClass.Compact) 
    }
    var itemToDelete by remember { mutableStateOf<LeaveRequestEntity?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Modern Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tr("İzin Talepleri", "Leave Requests"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("İzin ve mazeret bildirimleri", "Manage leaves and excuses"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = { showForm = !showForm },
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (windowWidthSizeClass != WindowWidthSizeClass.Compact) {
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedVisibility(
                            visible = showForm,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LeaveSubmitFormComponent(
                                isWide = true,
                                onSubmit = { start, end, newDesc, _, newType ->
                                    onSubmitRequest(newType, start, end, newDesc)
                                    showForm = false
                                }
                            ) {
                                showForm = false
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (dbRequests.isEmpty()) {
                            EmptyListMessage()
                        } else {
                            RequestList(dbRequests) { itemToDelete = it }
                        }
                    }
                } else {
                    // Mobile/Compact: Vertical layout, either Form or List
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(visible = showForm, enter = fadeIn(), exit = fadeOut()) {
                            LeaveSubmitFormComponent(
                                isWide = false,
                                onSubmit = { start, end, newDesc, _, newType ->
                                    onSubmitRequest(newType, start, end, newDesc)
                                    showForm = false
                                }
                            ) {
                                showForm = false
                            }
                        }

                        if (!showForm) {
                            if (dbRequests.isEmpty()) {
                                EmptyListMessage()
                            } else {
                                RequestList(dbRequests) { itemToDelete = it }
                            }
                        }
                    }
                }
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text(tr("İşlemi Onayla", "Confirm Action"), fontWeight = FontWeight.Bold) },
            text = { Text(tr("Bu talebi silmek istediğinize emin misiniz?", "Are you sure you want to delete this request?")) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRequest(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tr("Evet, Sil", "Yes, Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LeaveRequestScreenPreview() {
    SahaTakipTheme {
        LeaveRequestScreenContent(
            dbRequests = listOf(
                LeaveRequestEntity(
                    id = 1,
                    requestType = "Yıllık İzin",
                    startDate = "12.08.2026",
                    endDate = "15.08.2026",
                    reason = "Tatil planı",
                    status = "Beklemede"
                )
            ),
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSubmitRequest = { _, _, _, _ -> },
            onDeleteRequest = {}
        )
    }
}


@Composable
private fun EmptyListMessage() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(tr("Talep bulunamadı.", "No requests found."), color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun RequestList(
    requests: List<LeaveRequestEntity>,
    onDeleteItem: (LeaveRequestEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.id }) { item ->
            LeaveRequestCardItem(
                item = item,
                onDelete = { onDeleteItem(item) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveSubmitFormComponent(
    isWide: Boolean = true,
    onSubmit: (startDate: String, endDate: String, description: String, status: String, type: String) -> Unit,
    onCancel: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault()) }
    
    val today = remember { LocalDate.now() }
    val currentYearStart = remember { today.withDayOfYear(1) }

    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var descriptionInput by remember { mutableStateOf("") }
    
    val leaveTypes = listOf(tr("Mazeret İzni", "Excuse Leave"), tr("Yıllık İzin", "Annual Leave"), tr("Sağlık İzni", "Sick Leave"))
    var selectedType by remember { mutableStateOf(leaveTypes[0]) }
    var expandedType by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    fun LocalDate.toMillis(): Long = this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.toMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = utcTimeMillis.toLocalDate()

                    return !date.isBefore(currentYearStart) &&
                            !date.isAfter(endDate) &&
                            (ChronoUnit.DAYS.between(date, endDate) <= 365)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        val date = selected.toLocalDate()
                        startDate = date
                        if (endDate.isBefore(date)) {
                            endDate = date
                        }
                    }
                    showStartDatePicker = false
                }) {
                    Text(tr("Tamam", "OK"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.toMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = utcTimeMillis.toLocalDate()
                    return !date.isBefore(currentYearStart) &&
                            !date.isBefore(startDate) &&
                            (ChronoUnit.DAYS.between(startDate, date) <= 365)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        endDate = selected.toLocalDate()
                    }
                    showEndDatePicker = false
                }) {
                    Text(tr("Tamam", "OK"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(tr("Yeni Talep Oluştur", "Create New Request"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = !expandedType }) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(tr("İzin Türü", "Leave Type")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                    leaveTypes.forEach { type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = { selectedType = type; expandedType = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isWide) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }) {
                        OutlinedTextField(
                            value = startDate.format(dateFormatter),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text(tr("Başlangıç", "Start Date")) },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f).clickable { showEndDatePicker = true }) {
                        OutlinedTextField(
                            value = endDate.format(dateFormatter),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text(tr("Bitiş", "End Date")) },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true }) {
                        OutlinedTextField(
                            value = startDate.format(dateFormatter),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text(tr("Başlangıç", "Start Date")) },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().clickable { showEndDatePicker = true }) {
                        OutlinedTextField(
                            value = endDate.format(dateFormatter),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text(tr("Bitiş", "End Date")) },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { 
                    val lineCount = it.count { char -> char == '\n' } + 1
                    if ((it.length <= 100) && (lineCount <= 4)) {
                        descriptionInput = it
                    }
                },
                label = { Text(tr("Açıklama", "Description")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                supportingText = {
                    Text(
                        text = "${descriptionInput.length}/100",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                        color = if (descriptionInput.length >= 100) StatusRed else MaterialTheme.colorScheme.secondary
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(tr("İptal", "Cancel")) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { 
                        if (descriptionInput.isNotBlank()) { 
                            onSubmit(
                                startDate.format(dateFormatter), 
                                endDate.format(dateFormatter), 
                                descriptionInput, 
                                Constants.LEAVE_STATUS_PENDING, 
                                selectedType
                            ) 
                        } 
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tr("Kaydet", "Save"))
                }
            }
        }
    }
}

@Composable
fun LeaveRequestCardItem(item: LeaveRequestEntity, onDelete: () -> Unit) {
    val (color, icon) = when (item.status.uppercase(Locale.ROOT)) {
        Constants.LEAVE_STATUS_APPROVED -> StatusGreen to Icons.Default.Verified
        Constants.LEAVE_STATUS_REJECTED -> StatusRed to Icons.Default.Error
        else -> StatusAmber to Icons.Default.Pending
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.requestType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val dateText = if (item.startDate == item.endDate) item.startDate else "${item.startDate} - ${item.endDate}"
                    Text(text = dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = item.status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = item.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
