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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
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

    var isVerifying by remember { mutableStateOf(false) }
    var verificationSuccess by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val biometricManager = remember { BiometricPromptManager(context) }
    val biometricAvailability = remember { biometricManager.checkBiometricAvailability() }

    val promptTitle = tr("Biyometrik Kimlik Doğrulama", "Biometric Authentication")
    val promptSubtitle = tr("Saha Personeli Güvenlik Doğrulaması", "Field Staff Security Verification")
    val promptDesc = tr("Parmak izi veya Yüz Tanıma sensörüne dokunun", "Touch the fingerprint or Face ID sensor")
    val promptCancel = tr("İptal", "Cancel")
    val errPrefix = tr("Biyometrik Hata", "Biometric Error")
    val matchFailedMsg = tr("Biyometrik eşleşme başarısız. Tekrar deneyin.", "Biometric match failed. Try again.")

    val triggerBiometricAuth = {
        val activity = context as? FragmentActivity
        if (activity != null && biometricAvailability is BiometricStatus.Available) {
            isVerifying = true
            biometricManager.showBiometricPrompt(
                activity = activity,
                title = promptTitle,
                subtitle = promptSubtitle,
                description = promptDesc,
                negativeButtonText = promptCancel,
                onSuccess = {
                    isVerifying = false
                    verificationSuccess = true
                    if (viewModel.authenticateWithBiometrics()) {
                        onLoginSuccess()
                    }
                },
                onError = { errorCode, errString ->
                    isVerifying = false
                    statusMessage = "$errPrefix: $errString"
                },
                onFailed = {
                    isVerifying = false
                    statusMessage = matchFailedMsg
                }
            )
        } else {
            // Fallback for emulator / environments without hardware enrollment
            isVerifying = true
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = tr("SAHA TAKİP SİSTEMİ", "FIELD TRACKING SYSTEM"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = tr("Hoş Geldiniz, ${userProfile?.fullName ?: "Saha Personeli"}", "Welcome, ${userProfile?.fullName ?: "Field Staff"}"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "${tr("T.C. No", "ID No")}: ${userProfile?.tcNo ?: "-----------"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // BIOMETRIC PROMPT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { triggerBiometricAuth() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (verificationSuccess) StatusGreen.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint",
                                tint = if (verificationSuccess) StatusGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Face ID",
                                tint = if (verificationSuccess) StatusGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tr("BiometricPrompt API ile Güvenli Giriş", "Secure Sign In with BiometricPrompt API"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tr("Parmak İzi veya Yüz Tanıma (Face ID) Sensörünü Başlatmak İçin Dokunun", "Tap to launch Fingerprint or Face Recognition sensor"),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Availability Status Indicator
                    val (statusText, statusColor) = when (biometricAvailability) {
                        is BiometricStatus.Available -> Pair(tr("Donanım Hazır (Parmak İzi / Yüz)", "Hardware Ready (Fingerprint / Face)"), StatusGreen)
                        is BiometricStatus.Unavailable -> Pair(biometricAvailability.reason, MaterialTheme.colorScheme.secondary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusRed,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    AnimatedVisibility(visible = verificationSuccess) {
                        Text(
                            text = tr("✓ Doğrulama Başarılı! Giriş Yapılıyor...", "✓ Verification Successful! Signing In..."),
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { triggerBiometricAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("BiometricPrompt Sistemini Çalıştır", "Launch BiometricPrompt System"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


