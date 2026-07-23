package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.BiometricPromptManager
import com.example.util.BiometricStatus
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.tr
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentLang by viewModel.language.collectAsState()
    val currentInterval by viewModel.updateInterval.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val serverUrl by viewModel.mockServerUrl.collectAsState()

    var urlInput by remember { mutableStateOf(serverUrl) }
    var biometricTestResult by remember { mutableStateOf<String?>(null) }

    val bioManager = remember { BiometricPromptManager(context) }
    val bioAvailability = remember { bioManager.checkBiometricAvailability() }

    val bioTitle = tr("Biyometrik Sensör Testi", "Biometric Sensor Test")
    val bioSub = tr("Saha Güvenlik Birimi Doğrulaması", "Field Security Unit Verification")
    val bioDesc = tr("Parmak izi veya Yüz Tanıma sensörünüzü test edin", "Test your fingerprint or Face Recognition sensor")
    val bioCancel = tr("Kapat", "Close")
    val bioSuccessMsg = tr("✓ Biyometrik Sensör Doğrulaması Başarılı!", "✓ Biometric Sensor Verification Successful!")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = tr("Uygulama & Takip Ayarları", "Application & Tracking Settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tr("Dil, güncelleme sıklığı ve sunucu yapılandırması", "Language, update frequency, and server configuration"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. LANGUAGE SELECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Dil Seçimi (Language)", "Language Selection"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentLang == "tr",
                            onClick = { viewModel.setLanguage("tr") },
                            label = { Text("Türkçe (TR)") }
                        )
                        FilterChip(
                            selected = currentLang == "en",
                            onClick = { viewModel.setLanguage("en") },
                            label = { Text("English (EN)") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. GPS UPDATE INTERVAL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Konum Güncelleme Sıklığı", "Location Update Frequency"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tr("Arka planda GPS verisi alma periyodu (Varsayılan: 60s)", "Background GPS fetch interval (Default: 60s)"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 120, 300).forEach { seconds ->
                            FilterChip(
                                selected = currentInterval == seconds,
                                onClick = { viewModel.setUpdateInterval(seconds) },
                                label = { Text("${seconds}s") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. THEME SELECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Tema Tercihi", "Theme Preference"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentTheme == "system",
                            onClick = { viewModel.setTheme("system") },
                            label = { Text(tr("Sistem", "System")) }
                        )
                        FilterChip(
                            selected = currentTheme == "light",
                            onClick = { viewModel.setTheme("light") },
                            label = { Text(tr("Açık", "Light")) }
                        )
                        FilterChip(
                            selected = currentTheme == "dark",
                            onClick = { viewModel.setTheme("dark") },
                            label = { Text(tr("Koyu", "Dark")) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. MOCK SYNC ENDPOINT URL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Sunucu Senkronizasyon URL", "Server Synchronization URL"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("REST Sync Endpoint") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.setMockServerUrl(urlInput) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(tr("Güncelle", "Update"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. BIOMETRIC SECURITY SENSOR STATUS & TEST
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Biyometrik Güvenlik Sensör Testi", "Biometric Security Sensor Test"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tr("Parmak izi veya Yüz Tanıma (Face ID) donanım durumunu ve BiometricPrompt API çalışmasını test edin.", "Test fingerprint or Face ID hardware status and BiometricPrompt API execution."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val (statusText, statusColor) = when (bioAvailability) {
                        is BiometricStatus.Available -> Pair(tr("Donanım Aktif ve Hazır", "Hardware Active & Ready"), StatusGreen)
                        is BiometricStatus.Unavailable -> Pair(bioAvailability.reason, MaterialTheme.colorScheme.secondary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = statusColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    biometricTestResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = res,
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null && bioAvailability is BiometricStatus.Available) {
                                bioManager.showBiometricPrompt(
                                    activity = activity,
                                    title = bioTitle,
                                    subtitle = bioSub,
                                    description = bioDesc,
                                    negativeButtonText = bioCancel,
                                    onSuccess = {
                                        biometricTestResult = bioSuccessMsg
                                    },
                                    onError = { code, err ->
                                        biometricTestResult = "Hata ($code): $err"
                                    },
                                    onFailed = {
                                        biometricTestResult = "Doğrulama başarısız!"
                                    }
                                )
                            } else {
                                biometricTestResult = bioSuccessMsg
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("BiometricPrompt Sensörünü Test Et", "Test BiometricPrompt Sensor"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

