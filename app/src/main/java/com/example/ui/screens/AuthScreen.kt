package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricPromptManager
import com.example.util.BiometricStatus
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.MainViewModel
import com.example.util.IdCardPreset
import com.example.util.OcrCardScanner
import com.example.util.tr

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val ocrResult by viewModel.ocrScanningState.collectAsState()
    val ocrIsLoading by viewModel.ocrIsLoading.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()

    val bioTitle = tr("Biyometrik Hızlı Giriş", "Biometric Quick Login")
    val bioSub = tr("Saha Personeli Biyometrik Kimlik Doğrulama", "Field Staff Biometric Verification")
    val bioDesc = tr("Parmak izi veya Yüz Tanıma sensörüne dokunun", "Touch the fingerprint or Face ID sensor")
    val bioCancel = tr("İptal", "Cancel")

    var nameInput by remember { mutableStateOf("AHMET CAN YILMAZ") }
    var codeInput by remember { mutableStateOf("SAHA2026") }
    var showCameraModal by remember { mutableStateOf(false) }
    var showRawTextLog by remember { mutableStateOf(false) }

    // If OCR scan result updates, sync nameInput automatically
    ocrResult?.let { result ->
        if (nameInput != result.fullName) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Hero Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.saha_hero_banner_1784703591771),
                        contentDescription = "Header Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = tr("SAHA TAKİP SİSTEMİ", "FIELD TRACKING SYSTEM"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tr("Mobil Personel Kimlik Doğrulama & Aktivasyon", "Mobile Personnel Authentication & Activation"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STEP 1: ID CARD OCR SCANNER CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("1. Kimlik Kartı Tarama (OCR)", "1. ID Card Scan (OCR)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Cihaz kamerası ile T.C. Kimlik Kartı (Ön Yüz) tara", "Scan ID Card (Front Side) with device camera"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Camera Viewfinder Box / Trigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 2.dp,
                                color = if (ocrResult != null) StatusGreen else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showCameraModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (ocrIsLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr("Kimlik Kartı OCR ile Okunuyor...", "Reading ID Card via OCR..."),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else if (ocrResult != null) {
                            val res = ocrResult!!
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr("OCR Taraması Başarılı (%${(res.confidenceScore * 100).toInt()})", "OCR Scan Successful (%${(res.confidenceScore * 100).toInt()})"),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = res.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${tr("T.C. No", "ID No")}: ${res.tcNo} • ${tr("Seri No", "Serial")}: ${res.serialNo}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tr("Yeniden Taramak İçin Dokunun", "Tap to Scan Again"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = tr("Kamerayı Aç ve Kimlik Kartı Tara", "Open Camera & Scan ID Card"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr("T.C. Kimlik Kartı Ön Yüz Okuma", "T.C. ID Card Front Scan"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Extracted OCR Details Card (If scanned)
                    ocrResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tr("Ayrıştırılan Kart Bilgileri", "Extracted Card Details"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    TextButton(onClick = { showRawTextLog = !showRawTextLog }) {
                                        Text(
                                            text = if (showRawTextLog) tr("Gizle", "Hide") else tr("Ham Veri", "Raw Text"),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Icon(
                                            imageVector = if (showRawTextLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${tr("T.C. Kimlik No", "ID Number")}: ${res.tcNo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${tr("Doğum Tarihi", "Birth Date")}: ${res.birthDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${tr("Seri No", "Serial")}: ${res.serialNo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${tr("Son Geçerlilik", "Valid Until")}: ${res.validUntil}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showRawTextLog) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                        Text(
                                            text = tr("OCR & MRZ Metin Çıktısı:", "OCR & MRZ Text Output:"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = res.rawExtractedText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(tr("Ad Soyad (OCR Doğrulama)", "Full Name (OCR Verification)")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        trailingIcon = {
                            if (ocrResult != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = StatusGreen
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STEP 2: ONE-TIME ACTIVATION CODE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr("2. Aktivasyon Kodu", "2. Activation Code"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Kurum yönetimi tarafından verilen tek kullanımlık kod", "One-time activation code provided by administration"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text(tr("Aktivasyon Kodu (Örn: SAHA2026)", "Activation Code (E.g. SAHA2026)")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = tr("Varsayılan test kodu: SAHA2026 veya 123456", "Default test code: SAHA2026 or 123456"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusRed,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (viewModel.activateWithCode(codeInput)) {
                        onAuthSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Aktivasyonu Tamamla & Giriş Yap", "Complete Activation & Sign In"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    val biometricManager = BiometricPromptManager(context)
                    if (activity != null && biometricManager.checkBiometricAvailability() is BiometricStatus.Available) {
                        biometricManager.showBiometricPrompt(
                            activity = activity,
                            title = bioTitle,
                            subtitle = bioSub,
                            description = bioDesc,
                            negativeButtonText = bioCancel,
                            onSuccess = {
                                if (viewModel.authenticateWithBiometrics()) {
                                    onAuthSuccess()
                                }
                            },
                            onError = { _, _ -> },
                            onFailed = { }
                        )
                    } else {
                        if (viewModel.authenticateWithBiometrics()) {
                            onAuthSuccess()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Biyometrik Hızlı Giriş (Parmak İzi / Yüz Tanıma)", "Biometric Quick Sign In (Fingerprint / Face ID)"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // OCR CAMERA SCANNER MODAL DIALOG
    if (showCameraModal) {
        OcrCameraScannerModal(
            currentNameInput = nameInput,
            onDismiss = { showCameraModal = false },
            onScanStart = { preset ->
                viewModel.startIdCardOcrScan(fullNameInput = nameInput, preset = preset)
                showCameraModal = false
            }
        )
    }
}

@Composable
fun OcrCameraScannerModal(
    currentNameInput: String,
    onDismiss: () -> Unit,
    onScanStart: (preset: IdCardPreset?) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<IdCardPreset?>(OcrCardScanner.availablePresets[0]) }

    val infiniteTransition = rememberInfiniteTransition()
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("OCR Kimlik Kartı Kamerası", "OCR ID Card Camera"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(tr("Kapat", "Close"))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Live Viewfinder Frame
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // ID Card Mockup Frame in Center
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(220.dp)
                                .align(Alignment.Center)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            // ID Card Design Simulation inside frame
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tr("T.C. KİMLİK KARTI", "TURKEY IDENTITY CARD"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color.Yellow.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "OCR DETECTED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Yellow,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                val previewName = selectedPreset?.fullName ?: currentNameInput.ifBlank { "AHMET CAN YILMAZ" }
                                val previewTc = selectedPreset?.tcNo ?: "10293847562"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "TC: $previewTc",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.Green,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = previewName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "I<TUR$previewTc<<<<<<<<<<<<<<<9008124M3008122TUR<<<<<<<<<<<0",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }

                            // Moving Scan Line Animation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.TopStart)
                                    .padding(top = (217 * scanLineY).dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Red,
                                                Color.Yellow,
                                                Color.Red,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        // Top Overlay Hint
                        Text(
                            text = tr("Kimliğin ön yüzünü hizada tutun", "Keep ID card front aligned"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preset selector chips for quick testing
                Text(
                    text = tr("Taranacak Kart Örneği / Test Personeli:", "Card Sample to Scan / Test Staff:"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OcrCardScanner.availablePresets.chunked(2).forEach { rowPresets ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowPresets.forEach { preset ->
                                val isSelected = selectedPreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPreset = preset },
                                    label = { Text("${preset.title} (${preset.fullName.substringBefore(" ")})", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shutter Button
                Button(
                    onClick = { onScanStart(selectedPreset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Kameradan Tara & OCR Bilgilerini Oku", "Scan Camera & Read OCR Info"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


