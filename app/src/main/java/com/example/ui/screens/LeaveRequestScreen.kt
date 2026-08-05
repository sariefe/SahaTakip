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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.RequestLogViewModel
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    viewModel: RequestLogViewModel,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val dbRequests by viewModel.allLeaveRequests.collectAsStateWithLifecycle()

    var showForm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<com.example.data.local.entity.LeaveRequestEntity?>(null) }

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

            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                // Form Section (Compact)
                AnimatedVisibility(visible = showForm, enter = fadeIn(), exit = fadeOut()) {
                    LeaveSubmitFormComponent(
                        onSubmit = { start, end, newDesc, _, newType ->
                            viewModel.submitLeaveRequest(type = newType, startDate = start, endDate = end, reason = newDesc)
                            showForm = false
                        },
                        onCancel = { showForm = false }
                    )
                }

                if (!showForm) {
                    if (dbRequests.isEmpty()) {
                        EmptyListMessage()
                    } else {
                        RequestList(dbRequests) { itemToDelete = it }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        LeaveSubmitFormComponent(
                            onSubmit = { start, end, newDesc, _, newType ->
                                viewModel.submitLeaveRequest(type = newType, startDate = start, endDate = end, reason = newDesc)
                            },
                            onCancel = { /* No cancel needed in side-by-side */ }
                        )
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
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
                        viewModel.deleteLeaveRequest(item.id)
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

@Composable
private fun EmptyListMessage() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(tr("Talep bulunamadı.", "No requests found."), color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun RequestList(
    requests: List<com.example.data.local.entity.LeaveRequestEntity>,
    onDeleteItem: (com.example.data.local.entity.LeaveRequestEntity) -> Unit
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
    onSubmit: (startDate: String, endDate: String, description: String, status: String, type: String) -> Unit,
    onCancel: () -> Unit
) {
    val currentDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) }
    var startDateInput by remember { mutableStateOf(currentDateStr) }
    var endDateInput by remember { mutableStateOf(currentDateStr) }
    var descriptionInput by remember { mutableStateOf("") }
    
    val leaveTypes = listOf(tr("Mazeret İzni", "Excuse Leave"), tr("Yıllık İzin", "Annual Leave"), tr("Sağlık İzni", "Sick Leave"))
    var selectedType by remember { mutableStateOf(leaveTypes[0]) }
    var expandedType by remember { mutableStateOf(false) }

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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDateInput,
                    onValueChange = { startDateInput = it },
                    label = { Text(tr("Başlangıç", "Start Date")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = endDateInput,
                    onValueChange = { endDateInput = it },
                    label = { Text(tr("Bitiş", "End Date")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { 
                    val lineCount = it.count { char -> char == '\n' } + 1
                    if (it.length <= 100 && lineCount <= 4) {
                        descriptionInput = it
                    }
                },
                label = { Text(tr("Açıklama", "Description")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text(
                        text = "${descriptionInput.length}/100",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        color = if (descriptionInput.length >= 100) StatusRed else MaterialTheme.colorScheme.secondary
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(tr("İptal", "Cancel")) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (descriptionInput.isNotBlank()) onSubmit(startDateInput, endDateInput, descriptionInput, "BEKLEMEDE", selectedType) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tr("Kaydet", "Save"))
                }
            }
        }
    }
}

@Composable
fun LeaveRequestCardItem(item: com.example.data.local.entity.LeaveRequestEntity, onDelete: () -> Unit) {
    val (color, icon) = when (item.status.uppercase(Locale.ROOT)) {
        "ONAYLANDI" -> StatusGreen to Icons.Default.Verified
        "REDDEDİLDİ" -> StatusRed to Icons.Default.Error
        else -> StatusAmber to Icons.Default.Pending
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = item.requestType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val dateText = if (item.startDate == item.endDate) item.startDate else "${item.startDate} - ${item.endDate}"
                    Text(text = dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                
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
