package com.example.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.OcrAnalyzer
import com.example.util.OcrCardScanner
import com.example.util.StaffCardPreset
import com.example.util.tr
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrCameraScannerModal(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onScanStart: (preset: StaffCardPreset?) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val haptic = LocalHapticFeedback.current
    
    var selectedPreset by remember { mutableStateOf<StaffCardPreset?>(null) }
    val liveOcrResult by viewModel.ocrScanningState.collectAsStateWithLifecycle()
    val stability by viewModel.ocrStability.collectAsStateWithLifecycle()
    val detectedLines by viewModel.detectedLines.collectAsStateWithLifecycle()
    val imgWidth by viewModel.ocrImageWidth.collectAsStateWithLifecycle()
    val imgHeight by viewModel.ocrImageHeight.collectAsStateWithLifecycle()

    LaunchedEffect(stability) {
        if (stability >= 1.0f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val frameColor = when {
        stability < 0.2f -> Color.Red.copy(alpha = 0.5f)
        stability < 0.7f -> Color.Yellow.copy(alpha = 0.6f)
        else -> Color.Green.copy(alpha = 0.8f)
    }

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
                            text = tr("OCR Kimlik", "OCR ID"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(tr("Kapat", "Close"))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also {
                                            it.setAnalyzer(cameraExecutor, OcrAnalyzer { lines, w, h ->
                                                viewModel.onRealOcrDetected(lines, w, h)
                                            })
                                        }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    try {
                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )

                                        // Set autofocus on the central ROI
                                        val factory = previewView.meteringPointFactory
                                        val centerPoint = factory.createPoint(previewView.width / 2f, previewView.height / 2f)
                                        val action = androidx.camera.core.FocusMeteringAction.Builder(centerPoint)
                                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                            .build()
                                        camera.cameraControl.startFocusAndMetering(action)

                                    } catch (e: Exception) {
                                        android.util.Log.e("OcrCamera", "Camera binding failed", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Live Bounding Box Overlays
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = constraints.maxWidth.toFloat()
                            val canvasHeight = constraints.maxHeight.toFloat()

                            if (imgWidth > 0 && imgHeight > 0) {
                                val scaleX = canvasWidth / imgWidth
                                val scaleY = canvasHeight / imgHeight
                                
                                val drawColor = frameColor // Capture value to avoid recomposition inside draw

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    detectedLines.forEach { line ->
                                        val left = line.left * scaleX
                                        val top = line.top * scaleY
                                        val width = line.width * scaleX
                                        val height = line.height * scaleY
                                        
                                        drawRoundRect(
                                            color = drawColor.copy(alpha = 0.2f),
                                            topLeft = Offset(left, top),
                                            size = Size(width, height),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                        drawRoundRect(
                                            color = drawColor,
                                            topLeft = Offset(left, top),
                                            size = Size(width, height),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // ID Card Mockup Frame in Center (Semi-transparent overlay)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(220.dp)
                                .align(Alignment.Center)
                                .border(
                                    width = 3.dp,
                                    color = frameColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
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
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tr("Kimliğin ön yüzünü hizada tutun", "Keep ID card front aligned"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            liveOcrResult?.let { result ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${result.fullName} (${result.staffId})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Green,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                    label = { Text("${preset.title} (${preset.firstName})", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onScanStart(selectedPreset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (liveOcrResult != null && selectedPreset == null) 
                            Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (liveOcrResult != null && selectedPreset == null)
                            tr("Tespit Edilen Kimliği Onayla", "Confirm Detected Identity")
                        else
                            tr("Kameradan Tara & OCR Bilgilerini Oku", "Scan Camera & Read OCR Info"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (liveOcrResult != null && selectedPreset == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.clearOcrResult() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Sonucu Temizle ve Yeniden Tara", "Clear Result and Rescan"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
