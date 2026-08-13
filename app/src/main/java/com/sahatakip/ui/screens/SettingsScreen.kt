package com.sahatakip.ui.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.sahatakip.ui.theme.StatusGreen
import com.sahatakip.ui.theme.StatusRed
import com.sahatakip.ui.viewmodel.SettingsViewModel
import com.sahatakip.util.BiometricPromptManager
import com.sahatakip.util.BiometricStatus
import com.sahatakip.util.tr

import androidx.compose.ui.tooling.preview.Preview
import com.sahatakip.ui.theme.SahaTakipTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val currentLang by viewModel.language.collectAsStateWithLifecycle()
    val currentInterval by viewModel.updateInterval.collectAsStateWithLifecycle()
    val currentTheme by viewModel.theme.collectAsStateWithLifecycle()
    val serverUrl by viewModel.mockServerUrl.collectAsStateWithLifecycle()

    val bioManager = remember { BiometricPromptManager(context) }
    val bioAvailability = remember { bioManager.checkBiometricAvailability() }

    SettingsScreenContent(
        currentLang = currentLang,
        currentInterval = currentInterval,
        currentTheme = currentTheme,
        serverUrl = serverUrl,
        bioAvailability = bioAvailability,
        bioManager = bioManager,
        windowWidthSizeClass = windowWidthSizeClass,
        onSetLanguage = { viewModel.setLanguage(it) },
        onSetTheme = { viewModel.setTheme(it) },
        onSetUpdateInterval = { viewModel.setUpdateInterval(it) },
        onSetMockServerUrl = { viewModel.setMockServerUrl(it) }
    )
}

@Composable
fun SettingsScreenContent(
    currentLang: String,
    currentInterval: Int,
    currentTheme: String,
    serverUrl: String,
    bioAvailability: BiometricStatus,
    bioManager: BiometricPromptManager,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetUpdateInterval: (Int) -> Unit,
    onSetMockServerUrl: (String) -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (windowWidthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp)
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
                SettingsBody(
                    currentLang, currentInterval, currentTheme, serverUrl,
                    bioAvailability, bioManager, context,
                    onSetLanguage, onSetTheme, onSetUpdateInterval, onSetMockServerUrl,
                    isCompact = true
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppearanceSettings(currentLang, currentTheme, onSetLanguage, onSetTheme, isCompact = false)
                        Spacer(modifier = Modifier.height(24.dp))
                        SecuritySettings(bioAvailability, bioManager, context, isCompact = false)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        TrackingSettings(currentInterval, serverUrl, onSetUpdateInterval, onSetMockServerUrl, isCompact = false)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsBody(
    currentLang: String,
    currentInterval: Int,
    currentTheme: String,
    serverUrl: String,
    bioAvailability: BiometricStatus,
    bioManager: BiometricPromptManager,
    context: android.content.Context,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetUpdateInterval: (Int) -> Unit,
    onSetMockServerUrl: (String) -> Unit,
    isCompact: Boolean = false
) {
    AppearanceSettings(currentLang, currentTheme, onSetLanguage, onSetTheme, isCompact)
    Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))
    TrackingSettings(currentInterval, serverUrl, onSetUpdateInterval, onSetMockServerUrl, isCompact)
    Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))
    SecuritySettings(bioAvailability, bioManager, context, isCompact)
}

@Composable
private fun AppearanceSettings(
    currentLang: String,
    currentTheme: String,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    isCompact: Boolean = false
) {
    SettingsSectionHeader(tr("Görünüm ve Dil", "Appearance & Language"), isCompact)
    SettingsCard(isCompact = isCompact) {
        SettingsRowItem(
            title = tr("Dil", "Language"),
            icon = Icons.Default.Language,
            isCompact = isCompact
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)) {
                LanguageChip("TR", currentLang == "tr", isCompact) { onSetLanguage("tr") }
                LanguageChip("EN", currentLang == "en", isCompact) { onSetLanguage("en") }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = if (isCompact) 8.dp else 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsRowItem(
            title = tr("Tema Seçimi", "Theme Selection"),
            icon = Icons.Default.Palette,
            isCompact = isCompact
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)) {
                ThemeIconChip(Icons.Default.SettingsBrightness, currentTheme == "system", isCompact) { onSetTheme("system") }
                ThemeIconChip(Icons.Default.LightMode, currentTheme == "light") { onSetTheme("light") }
                ThemeIconChip(Icons.Default.DarkMode, currentTheme == "dark") { onSetTheme("dark") }
            }
        }
    }
}

@Composable
private fun TrackingSettings(
    currentInterval: Int,
    serverUrl: String,
    onSetUpdateInterval: (Int) -> Unit,
    onSetMockServerUrl: (String) -> Unit,
    isCompact: Boolean = false
) {
    var urlInput by remember { mutableStateOf(serverUrl) }
    SettingsSectionHeader(tr("Takip Yapılandırması", "Tracking Configuration"), isCompact)
    SettingsCard(isCompact = isCompact) {
        SettingsRowItem(
            title = tr("Kontrol Sıklığı", "Control Interval"),
            icon = Icons.Default.Timer,
            isCompact = isCompact
        ) {
            Column(horizontalAlignment = Alignment.End) {
                val intervals = listOf(30, 60, 120, 300)
                intervals.chunked(if (isCompact) 4 else 2).forEach { rowIntervals ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        rowIntervals.forEach { sec ->
                            val isSelected = currentInterval == sec
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onSetUpdateInterval(sec) }
                                    .padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = if (isCompact) 4.dp else 6.dp),
                                color = Color.Transparent
                            ) {
                                Text(
                                    "${sec}s",
                                    style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = if (isCompact) 8.dp else 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                Text(tr("Senkronizasyon Sunucusu", "Sync Server"), style = if (isCompact) MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp) else MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = if (isCompact) MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp) else MaterialTheme.typography.bodySmall,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
            Button(
                onClick = { onSetMockServerUrl(urlInput) },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(tr("Güncelle", "Update"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    SahaTakipTheme {
        SettingsScreenContent(
            currentLang = "tr",
            currentInterval = 60,
            currentTheme = "system",
            serverUrl = "https://mock.server.com.tr",
            bioAvailability = BiometricStatus.Available,
            bioManager = BiometricPromptManager(context),
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSetLanguage = {},
            onSetTheme = {},
            onSetUpdateInterval = {},
            onSetMockServerUrl = {}
        )
    }
}


@Composable
private fun SecuritySettings(
    bioAvailability: BiometricStatus,
    bioManager: BiometricPromptManager,
    context: android.content.Context,
    isCompact: Boolean = false
) {
    var biometricTestResult by remember { mutableStateOf<String?>(null) }
    val sensorTestTitle = tr("Sensör Testi", "Sensor Test")
    val authSuccessMsg = tr("Doğrulama Başarılı", "Auth Successful")
    val failedMsg = tr("Başarısız", "Failed")
    val simulatedMsg = tr("Simüle Edildi", "Simulated Success")

    SettingsSectionHeader(tr("Güvenlik ve Tanılama", "Security & Diagnostics"), isCompact)
    SettingsCard(isCompact = isCompact) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                    Text(tr("Biyometrik Test", "Biometric Test"), style = if (isCompact) MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp) else MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                val (statusText, statusColor) = when (bioAvailability) {
                    is BiometricStatus.Available -> tr("Hazır", "Ready") to StatusGreen
                    is BiometricStatus.Unavailable -> tr("Pasif", "Unavailable") to StatusRed
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(statusText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))
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
                Text(tr("Sensörü Şimdi Test Et", "Test Sensor Now"), style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, isCompact: Boolean = false) {
    Text(
        text = title,
        style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = if (isCompact) 8.dp else 12.dp)
    )
}

@Composable
fun SettingsCard(isCompact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isCompact) 20.dp else 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 16.dp else 20.dp), content = content)
    }
}

@Composable
fun SettingsRowItem(
    title: String,
    icon: ImageVector,
    isCompact: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
            Text(
                text = title,
                style = if (isCompact) MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp) else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        content()
    }
}

@Composable
fun LanguageChip(label: String, isSelected: Boolean, isCompact: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 4.dp else 6.dp),
        color = Color.Transparent
    ) {
        Text(
            label,
            style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThemeIconChip(icon: ImageVector, isSelected: Boolean, isCompact: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(if (isCompact) 32.dp else 36.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (isCompact) 16.dp else 18.dp), tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
