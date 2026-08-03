package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BiometricPromptManager
import com.example.util.BiometricStatus
import com.example.util.tr

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val currentLang by viewModel.language.collectAsState()
    val currentInterval by viewModel.updateInterval.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val serverUrl by viewModel.mockServerUrl.collectAsState()

    val bioManager = remember { BiometricPromptManager(context) }
    val bioAvailability = remember { bioManager.checkBiometricAvailability() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = tr("Ayarlar", "Settings"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr("Uygulama tercihlerini yönetin", "Manage app preferences"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                SettingsContent(viewModel, currentLang, currentInterval, currentTheme, serverUrl, bioAvailability, bioManager, context)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppearanceSettings(viewModel, currentLang, currentTheme)
                        Spacer(modifier = Modifier.height(24.dp))
                        SecuritySettings(bioAvailability, bioManager, context)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        TrackingSettings(viewModel, currentInterval, serverUrl)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SettingsContent(
    viewModel: MainViewModel,
    currentLang: String,
    currentInterval: Int,
    currentTheme: String,
    serverUrl: String,
    bioAvailability: BiometricStatus,
    bioManager: BiometricPromptManager,
    context: android.content.Context
) {
    AppearanceSettings(viewModel, currentLang, currentTheme)
    Spacer(modifier = Modifier.height(24.dp))
    TrackingSettings(viewModel, currentInterval, serverUrl)
    Spacer(modifier = Modifier.height(24.dp))
    SecuritySettings(bioAvailability, bioManager, context)
}

@Composable
private fun AppearanceSettings(viewModel: MainViewModel, currentLang: String, currentTheme: String) {
    SettingsSectionHeader(tr("Görünüm ve Dil", "Appearance & Language"))
    SettingsCard {
        SettingsRowItem(
            title = tr("Dil", "Language"),
            icon = Icons.Default.Language
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChip("TR", currentLang == "tr") { viewModel.setLanguage("tr") }
                LanguageChip("EN", currentLang == "en") { viewModel.setLanguage("en") }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRowItem(
            title = tr("Tema Seçimi", "Theme Selection"),
            icon = Icons.Default.Palette
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeIconChip(Icons.Default.SettingsBrightness, currentTheme == "system") { viewModel.setTheme("system") }
                ThemeIconChip(Icons.Default.LightMode, currentTheme == "light") { viewModel.setTheme("light") }
                ThemeIconChip(Icons.Default.DarkMode, currentTheme == "dark") { viewModel.setTheme("dark") }
            }
        }
    }
}

@Composable
private fun TrackingSettings(viewModel: MainViewModel, currentInterval: Int, serverUrl: String) {
    var urlInput by remember { mutableStateOf(serverUrl) }
    SettingsSectionHeader(tr("Takip Yapılandırması", "Tracking Configuration"))
    SettingsCard {
        SettingsRowItem(
            title = tr("Kontrol Sıklığı", "Control Interval"),
            icon = Icons.Default.Timer
        ) {
            Column(horizontalAlignment = Alignment.End) {
                val intervals = listOf(30, 60, 120, 300)
                intervals.chunked(2).forEach { rowIntervals ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        rowIntervals.forEach { sec ->
                            val isSelected = currentInterval == sec
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.setUpdateInterval(sec) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.Transparent
                            ) {
                                Text(
                                    "${sec}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(tr("Senkronizasyon Sunucusu", "Sync Server"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
            Button(
                onClick = { viewModel.setMockServerUrl(urlInput) },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(tr("Güncelle", "Update"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SecuritySettings(bioAvailability: BiometricStatus, bioManager: BiometricPromptManager, context: android.content.Context) {
    var biometricTestResult by remember { mutableStateOf<String?>(null) }
    val sensorTestTitle = tr("Sensör Testi", "Sensor Test")
    val authSuccessMsg = tr("Doğrulama Başarılı", "Auth Successful")
    val failedMsg = tr("Başarısız", "Failed")
    val simulatedMsg = tr("Simüle Edildi", "Simulated Success")

    SettingsSectionHeader(tr("Güvenlik ve Tanılama", "Security & Diagnostics"))
    SettingsCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(tr("Biyometrik Test", "Biometric Test"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                val (statusText, statusColor) = when (bioAvailability) {
                    is BiometricStatus.Available -> tr("Hazır", "Ready") to StatusGreen
                    is BiometricStatus.Unavailable -> tr("Pasif", "Unavailable") to StatusRed
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(statusText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(tr("Sensörün çalışmasını ve yetkilendirme akışını test edin.", "Test sensor functionality and auth flow."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            biometricTestResult?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = if (it.contains("✓")) StatusGreen else StatusRed, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null && bioAvailability is BiometricStatus.Available) {
                        bioManager.showBiometricPrompt(
                            context = activity,
                            title = sensorTestTitle,
                            onSuccess = { biometricTestResult = "✓ $authSuccessMsg" },
                            onError = { _, err -> biometricTestResult = "Hata: $err" },
                            onFailed = { biometricTestResult = failedMsg }
                        )
                    } else {
                        biometricTestResult = "✓ $simulatedMsg"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(tr("Sensörü Şimdi Test Et", "Test Sensor Now"))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SettingsRowItem(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
fun LanguageChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = Color.Transparent
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ThemeIconChip(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
