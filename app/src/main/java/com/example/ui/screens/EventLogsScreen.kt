package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.local.entity.EventLogEntity
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.RequestLogViewModel
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventLogsScreen(
    viewModel: RequestLogViewModel,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val eventLogs by viewModel.allEventLogs.collectAsStateWithLifecycle()
    var selectedLogForNote by remember { mutableStateOf<EventLogEntity?>(null) }
    var noteInputText by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Refined Header
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(
                    text = tr("Olay Günlüğü", "Event History"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${eventLogs.size} ${tr("kayıt mevcut", "logs recorded")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (eventLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr("Henüz kaydedilmiş olay günlüğü yok.", "No event logs recorded yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(eventLogs) { log ->
                            EventLogCard(
                                log = log,
                                dateFormat = dateFormat,
                                onAddNote = {
                                    selectedLogForNote = log
                                    noteInputText = log.note
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(eventLogs) { log ->
                            EventLogCard(
                                log = log,
                                dateFormat = dateFormat,
                                onAddNote = {
                                    selectedLogForNote = log
                                    noteInputText = log.note
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ADD NOTE DIALOG - Modern Style
    selectedLogForNote?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogForNote = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(tr("Not Ekle", "Add Note"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(log.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { 
                            val lineCount = it.count { char -> char == '\n' } + 1
                            if (it.length <= 100 && lineCount <= 4) {
                                noteInputText = it
                            }
                        },
                        label = { Text(tr("Açıklama", "Explanation")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = false,
                            hintLocales = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("tr-TR"))
                        ),
                        supportingText = {
                            Text(
                                text = "${noteInputText.length}/100",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                color = if (noteInputText.length >= 100) StatusRed else MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNoteToEventLog(log.id, noteInputText)
                        selectedLogForNote = null
                    }
                ) {
                    Text(tr("Kaydet", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLogForNote = null }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        )
    }
}

@Composable
fun EventLogCard(
    log: EventLogEntity,
    dateFormat: SimpleDateFormat,
    onAddNote: () -> Unit
) {
    val badgeColor = when (log.status) {
        "TEHLİKE" -> StatusRed
        "UYARI" -> StatusAmber
        else -> StatusGreen
    }

    val icon = when (log.type) {
        "GEOFENCE_VIOLATION" -> Icons.Default.GppBad
        "INTERNET_LOST" -> Icons.Default.WifiOff
        "GPS_DISABLED" -> Icons.Default.GpsOff
        "SYNC_SUCCESS" -> Icons.Default.CloudDone
        else -> Icons.Default.Info
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
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = log.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Surface(
                    color = badgeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val statusText = when (log.status) {
                        "TEHLİKE" -> tr("TEHLİKE", "DANGER")
                        "UYARI" -> tr("UYARI", "WARNING")
                        "BİLGİ" -> tr("BİLGİ", "INFO")
                        "BAŞARILI" -> tr("BAŞARILI", "SUCCESS")
                        else -> log.status
                    }
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = log.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (log.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = log.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onAddNote,
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (log.note.isBlank()) tr("Not Ekle", "Add Note") else tr("Düzenle", "Edit"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
