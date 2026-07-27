package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.components.OcrCameraScannerModal
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BiometricPromptManager
import com.example.util.BiometricStatus
import com.example.util.tr

@Composable
fun BiometricLockScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val ocrAuthError by viewModel.ocrAuthError.collectAsState()

    var showOcrModal by remember { mutableStateOf(false) }
    var verificationSuccess by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    val biometricManager = remember { BiometricPromptManager(context) }
    val biometricAvailability = remember { biometricManager.checkBiometricAvailability() }

    val promptTitle = stringResource(R.string.biometric_title)
    val promptSubtitle = stringResource(R.string.biometric_subtitle)
    val promptDesc = stringResource(R.string.biometric_desc)
    val promptCancel = stringResource(R.string.biometric_cancel)
    val errPrefix = stringResource(R.string.biometric_error)
    val matchFailedMsg = tr("Biyometrik eşleşme başarısız. Tekrar deneyin.", "Biometric match failed. Try again.")

    val triggerBiometricAuth = {
        val activity = context as? FragmentActivity
        if (activity != null && biometricAvailability is BiometricStatus.Available) {
            biometricManager.showBiometricPrompt(
                activity = activity,
                title = promptTitle,
                subtitle = promptSubtitle,
                description = promptDesc,
                negativeButtonText = promptCancel,
                onSuccess = {
                    verificationSuccess = true
                    if (viewModel.authenticateWithBiometrics()) {
                        onLoginSuccess()
                    }
                },
                onError = { _, errString ->
                    biometricErrorMessage = "$errPrefix: $errString"
                },
                onFailed = {
                    biometricErrorMessage = matchFailedMsg
                }
            )
        } else {
            verificationSuccess = true
            if (viewModel.authenticateWithBiometrics()) {
                onLoginSuccess()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
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
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = tr("SAHA TAKİP SİSTEMİ", "FIELD TRACKING SYSTEM"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = tr("Hoş Geldiniz, ${userProfile?.fullName ?: "Saha Personeli"}", "Welcome, ${userProfile?.fullName ?: "Field Staff"}"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOcrModal = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "OCR Scanner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = tr("Kimlik Kartı ile Giriş", "Sign In with ID Card"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Fiziksel T.C. Kimlik Kartını Tara", "Scan Physical T.C. Identity Card"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ocrAuthError?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusRed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OPTION 2: BIOMETRIC LOGIN CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { triggerBiometricAuth() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = tr("Biyometrik Hızlı Giriş", "Biometric Quick Sign In"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Parmak İzi veya Yüz Tanıma", "Fingerprint or Face ID"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        biometricErrorMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusRed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(visible = verificationSuccess) {
                Text(
                    text = tr("✓ Doğrulama Başarılı! Giriş Yapılıyor...", "✓ Verification Successful! Signing In..."),
                    style = MaterialTheme.typography.labelLarge,
                    color = StatusGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showOcrModal) {
        OcrCameraScannerModal(
            viewModel = viewModel,
            onDismiss = { showOcrModal = false },
            onScanStart = { preset ->
                if (preset != null) {
                    if (viewModel.authenticateWithOcr(preset.tcNo)) {
                        verificationSuccess = true
                        showOcrModal = false
                        onLoginSuccess()
                    }
                } else {
                    val currentScannedTc = viewModel.ocrScanningState.value?.tcNo
                    if (currentScannedTc != null && viewModel.authenticateWithOcr(currentScannedTc)) {
                        verificationSuccess = true
                        showOcrModal = false
                        onLoginSuccess()
                    }
                }
            }
        )
    }
    LaunchedEffect(viewModel.isAuthenticated.collectAsState().value) {
        if (viewModel.isAuthenticated.value) {
            verificationSuccess = true
            showOcrModal = false
            onLoginSuccess()
        }
    }
}
