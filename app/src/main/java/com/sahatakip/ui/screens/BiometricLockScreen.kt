package com.sahatakip.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahatakip.ui.components.OcrCameraScannerModal
import com.sahatakip.ui.theme.SahaTakipTheme
import com.sahatakip.ui.theme.StatusGreen
import com.sahatakip.ui.theme.StatusRed
import com.sahatakip.ui.viewmodel.AuthViewModel
import com.sahatakip.ui.viewmodel.TrackingViewModel
import com.sahatakip.util.BiometricPromptManager
import com.sahatakip.util.BiometricStatus
import com.sahatakip.util.ScannedStaffCardResult
import com.sahatakip.util.tr

@Composable
fun BiometricLockScreen(
    viewModel: TrackingViewModel,
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ocrAuthError by authViewModel.ocrAuthError.collectAsStateWithLifecycle()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsStateWithLifecycle()

    var showOcrModal by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var verificationInProgress by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    val biometricManager = remember { BiometricPromptManager(context) }
    val bioAvailability = remember { biometricManager.checkBiometricAvailability() }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showOcrModal = true
        } else {
            showPermissionDeniedDialog = true
        }
    }

    val promptTitle = tr("Biyometrik Kimlik Doğrulama", "Biometric Authentication")
    val promptSubtitle = tr("Saha personeli güvenli giriş doğrulaması", "Field personnel secure login verification")
    val promptDesc = tr("Devam etmek için sensörü kullanın", "Use sensor to continue")
    val promptCancel = tr("İptal", "Cancel")
    val matchFailedTr = tr("Biyometrik eşleşme başarısız.", "Biometric match failed.")

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onLoginSuccess()
        }
    }

    BiometricLockScreenContent(
        userProfileFullName = userProfile?.fullName,
        ocrAuthError = ocrAuthError,
        biometricErrorMessage = biometricErrorMessage,
        verificationInProgress = verificationInProgress || isAuthenticated,
        onBiometricClick = {
            if (bioAvailability is BiometricStatus.Available) {
                biometricManager.showBiometricPrompt(
                    context = context,
                    title = promptTitle,
                    subtitle = promptSubtitle,
                    description = promptDesc,
                    negativeButtonText = promptCancel,
                    onSuccess = {
                        verificationInProgress = true
                        if (authViewModel.authenticateWithBiometrics()) {
                            onLoginSuccess()
                        }
                    },
                    onError = { _, errString -> 
                        biometricErrorMessage = errString
                        verificationInProgress = false
                    },
                    onFailed = {
                        biometricErrorMessage = matchFailedTr
                        verificationInProgress = false
                    }
                )
            } else {
                biometricErrorMessage = (bioAvailability as? BiometricStatus.Unavailable)?.reason ?: "Biyometrik hata."
            }
        },
        onOcrClick = {
            if (com.sahatakip.util.PermissionUtils.hasCameraPermission(context)) {
                showOcrModal = true
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    )

    if (showOcrModal) {
        val currentOcrResult by authViewModel.ocrScanningState.collectAsStateWithLifecycle()
        OcrCameraScannerModal(
            viewModel = authViewModel,
            onDismiss = { showOcrModal = false },
            onScanStart = { preset ->
                val idToAuth = preset?.staffId ?: currentOcrResult?.staffId
                if (idToAuth != null) {
                    val ocrData = currentOcrResult ?: preset?.let {
                        ScannedStaffCardResult(
                            firstName = it.firstName,
                            lastName = it.lastName,
                            staffId = it.staffId,
                            department = it.department
                        )
                    }
                    if (authViewModel.authenticateWithOcr(idToAuth, ocrData)) {
                        showOcrModal = false
                        onLoginSuccess()
                    }
                }
            }
        )
    }

    if (showPermissionDeniedDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text(tr("Kamera İzni Gerekli", "Camera Permission Required")) },
            text = {
                Text(
                    tr(
                        "Personel kartı doğrulama özelliğini kullanabilmek için kamera izni vermeniz gerekmektedir. Lütfen ayarlardan kamera iznini aktif edin.",
                        "You need to grant camera permission to use the staff card verification feature. Please enable camera permission in settings."
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    showPermissionDeniedDialog = false
                    com.sahatakip.util.PermissionUtils.openAppSettings(context)
                }) {
                    Text(tr("Ayarlara Git", "Go to Settings"))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text(tr("İptal", "Cancel"))
                }
            }
        )
    }
}

@Composable
fun BiometricLockScreenContent(
    userProfileFullName: String?,
    ocrAuthError: String?,
    biometricErrorMessage: String?,
    verificationInProgress: Boolean,
    onBiometricClick: () -> Unit,
    onOcrClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = tr("GÜVENLİ GİRİŞ", "SECURE ACCESS"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = tr("Hoş Geldiniz,", "Welcome back,"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Text(
                    text = userProfileFullName ?: tr("Saha Personeli", "Field Staff"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AuthMethodCard(
                        title = tr("Biyometrik Giriş", "Biometric Login"),
                        description = tr("Parmak İzi / Yüz Tanıma", "Fingerprint / Face ID"),
                        icon = Icons.Default.Fingerprint,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        onClick = onBiometricClick
                    )

                    AuthMethodCard(
                        title = tr("Kimlik Kartı Tara", "Scan ID Card"),
                        description = tr("Fiziksel Kart Doğrulaması", "Physical Card Verification"),
                        icon = Icons.Default.DocumentScanner,
                        primaryColor = MaterialTheme.colorScheme.secondary,
                        onClick = onOcrClick
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ocrAuthError?.let { msg ->
                    Text(msg, color = StatusRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                biometricErrorMessage?.let { msg ->
                    Text(msg, color = StatusRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                AnimatedVisibility(visible = verificationInProgress) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tr("Doğrulanıyor...", "Verifying..."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BiometricLockScreenPreview() {
    SahaTakipTheme {
        BiometricLockScreenContent(
            userProfileFullName = "Ahmet Can Yılmaz",
            ocrAuthError = null,
            biometricErrorMessage = null,
            verificationInProgress = false,
            onBiometricClick = {},
            onOcrClick = {}
        )
    }
}

@Composable
fun AuthMethodCard(
    title: String,
    description: String,
    icon: ImageVector,
    primaryColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
