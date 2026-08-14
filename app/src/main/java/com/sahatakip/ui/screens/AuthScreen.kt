package com.sahatakip.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahatakip.ui.components.OcrCameraScannerModal
import com.sahatakip.ui.theme.SahaTakipTheme
import com.sahatakip.ui.theme.StatusGreen
import com.sahatakip.ui.theme.StatusRed
import com.sahatakip.ui.viewmodel.AuthViewModel
import com.sahatakip.util.BiometricPromptManager
import com.sahatakip.util.BiometricStatus
import com.sahatakip.util.ScannedStaffCardResult
import com.sahatakip.util.tr
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val ocrResult by viewModel.ocrScanningState.collectAsStateWithLifecycle()
    val ocrIsLoading by viewModel.ocrIsLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val ocrScanSuggested by viewModel.ocrScanSuggested.collectAsStateWithLifecycle()

    var showCameraModal by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraModal = true
        }
    }

    LaunchedEffect(Unit) {
        if (!ocrScanSuggested && ocrResult == null) {
            delay(800.milliseconds)
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showCameraModal = true
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            viewModel.markOcrScanSuggested()
        }
    }

    AuthScreenContent(
        ocrResult = ocrResult,
        ocrIsLoading = ocrIsLoading,
        errorMessage = errorMessage,
        onActivate = { code ->
            if (viewModel.activateWithCode(code)) {
                onAuthSuccess()
            }
        },
        onBiometricAuth = {
            if (viewModel.authenticateWithBiometrics()) onAuthSuccess()
        },
        onLaunchCamera = { showCameraModal = true }
    )

    if (showCameraModal) {
        OcrCameraScannerModal(
            viewModel = viewModel,
            onDismiss = { showCameraModal = false },
            onScanStart = { preset ->
                viewModel.startIdCardOcrScan(preset = preset)
                showCameraModal = false
            }
        )
    }
}

@Composable
fun AuthScreenContent(
    ocrResult: ScannedStaffCardResult?,
    ocrIsLoading: Boolean,
    errorMessage: String?,
    onActivate: (String) -> Unit,
    onBiometricAuth: () -> Unit,
    onLaunchCamera: () -> Unit
) {
    val context = LocalContext.current
    val defaultName = tr("AHMET CAN YILMAZ", "AHMET CAN YILMAZ")
    var nameInput by remember { mutableStateOf(ocrResult?.fullName ?: defaultName) }
    var codeInput by remember { mutableStateOf("SAHA2026") }

    val bioTitle = tr("Biyometrik Kayıt", "Biometric Registration")
    val bioSub = tr("Saha Güvenlik Birimi", "Field Security Unit")
    val bioDesc = tr("Cihaz sensörünü kullanarak kimlik doğrulayın", "Use device sensor to verify identity")
    val bioCancel = tr("İptal", "Cancel")

    LaunchedEffect(ocrResult) {
        ocrResult?.let { result ->
            nameInput = result.fullName
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Blue.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = tr("SAHA TAKİP", "FIELD TRACKING"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = tr("Personel Aktivasyon", "Staff Activation"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AuthStepHeader(
                number = "1",
                title = tr("Personel Kartı Tarama", "Staff Card Scanning"),
                subtitle = tr("Kurum personel kartınızı kameraya gösterin", "Scan your organization staff card")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (ocrResult != null) StatusGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onLaunchCamera() },
                contentAlignment = Alignment.Center
            ) {
                if (ocrIsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                } else if (ocrResult != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(ocrResult.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${ocrResult.department} | ${ocrResult.staffId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(tr("Kart Doğrulandı", "Card Verified"), style = MaterialTheme.typography.labelSmall, color = StatusGreen)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            tr("Kamerayı Başlat", "Launch Camera"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AuthStepHeader(
                number = "2",
                title = tr("Aktivasyon Kodu", "Activation Code"),
                subtitle = tr("Kurumunuzdan aldığınız kodu girin", "Enter the code from your organization")
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 8) codeInput = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                placeholder = { Text("Şifre Girin") },
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )

            errorMessage?.let {
                Text(it, color = StatusRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { onActivate(codeInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    tr("AKTİVASYONU TAMAMLA", "COMPLETE ACTIVATION"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    val biometricManager = BiometricPromptManager(context)
                    if (biometricManager.checkBiometricAvailability() is BiometricStatus.Available) {
                        biometricManager.showBiometricPrompt(
                            context = context,
                            title = bioTitle,
                            subtitle = bioSub,
                            description = bioDesc,
                            negativeButtonText = bioCancel,
                            onSuccess = { onBiometricAuth() },
                            onError = { _, _ -> },
                            onFailed = { }
                        )
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Biyometrik Hızlı Giriş", "Biometric Quick Login"), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AuthStepHeader(number: String, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    SahaTakipTheme {
        AuthScreenContent(
            ocrResult = ScannedStaffCardResult("AHMET CAN", "YILMAZ", "12345", "SAHA"),
            ocrIsLoading = false,
            errorMessage = null,
            onActivate = {},
            onBiometricAuth = {},
            onLaunchCamera = {}
        )
    }
}
