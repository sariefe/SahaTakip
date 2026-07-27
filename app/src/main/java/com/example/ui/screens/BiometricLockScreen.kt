package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.OcrCameraScannerModal
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
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var showOcrModal by remember { mutableStateOf(false) }
    var verificationSuccess by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    val biometricManager = remember { BiometricPromptManager(context) }
    val bioAvailability = remember { biometricManager.checkBiometricAvailability() }

    val promptTitle = tr("Biyometrik Kimlik Doğrulama", "Biometric Authentication")
    val promptSubtitle = tr("Saha personeli güvenli giriş doğrulaması", "Field personnel secure login verification")
    val promptDesc = tr("Devam etmek için sensörü kullanın", "Use sensor to continue")
    val promptCancel = tr("İptal", "Cancel")
    
    val matchFailedTr = tr("Biyometrik eşleşme başarısız.", "Biometric match failed.")

    val triggerBiometricAuth = {
        val activity = context as? FragmentActivity
        if (activity != null && bioAvailability is BiometricStatus.Available) {
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
                    biometricErrorMessage = errString
                },
                onFailed = {
                    biometricErrorMessage = matchFailedTr
                }
            )
        } else {
            verificationSuccess = true
            if (viewModel.authenticateWithBiometrics()) {
                onLoginSuccess()
            }
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            verificationSuccess = true
            onLoginSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Subtle Gradient Background
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
                // High-Security Icon Circle
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
                            imageVector = Icons.Default.Shield,
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
                    text = userProfile?.fullName ?: tr("Saha Personeli", "Field Staff"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // DUAL LOGIN METHODS CONTAINER
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Option 1: Biometric (Primary)
                    AuthMethodCard(
                        title = tr("Biyometrik Giriş", "Biometric Login"),
                        description = tr("Parmak İzi / Yüz Tanıma", "Fingerprint / Face ID"),
                        icon = Icons.Default.Fingerprint,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        onClick = { triggerBiometricAuth() }
                    )

                    // Option 2: OCR (Secondary/Fallback)
                    AuthMethodCard(
                        title = tr("Kimlik Kartı Tara", "Scan ID Card"),
                        description = tr("Fiziksel Kart Doğrulaması", "Physical Card Verification"),
                        icon = Icons.Default.DocumentScanner,
                        primaryColor = MaterialTheme.colorScheme.secondary,
                        onClick = { showOcrModal = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error Messages
                ocrAuthError?.let { msg ->
                    Text(msg, color = StatusRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                biometricErrorMessage?.let { msg ->
                    Text(msg, color = StatusRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                AnimatedVisibility(visible = verificationSuccess) {
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

    if (showOcrModal) {
        OcrCameraScannerModal(
            viewModel = viewModel,
            onDismiss = { showOcrModal = false },
            onScanStart = { preset ->
                if (preset != null) {
                    if (viewModel.authenticateWithOcr(preset.tcNo)) {
                        showOcrModal = false
                        onLoginSuccess()
                    }
                }
            }
        )
    }
}

@Composable
fun AuthMethodCard(
    title: String,
    description: String,
    icon: ImageVector,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
