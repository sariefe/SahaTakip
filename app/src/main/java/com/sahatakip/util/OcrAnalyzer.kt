package com.sahatakip.util

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrAnalyzer(
    private val onTextDetected: (List<OcrLine>, Int, Int) -> Unit,
) : ImageAnalysis.Analyzer {

    companion object {
        private const val ROI_X_MIN = 0.05f
        private const val ROI_X_MAX = 0.95f
        private const val ROI_Y_MIN = 0.35f
        private const val ROI_Y_MAX = 0.65f
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalysisTimestamp = 0L
    private val analysisInterval = 200L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastAnalysisTimestamp) < analysisInterval) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            
            val imgWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
            val imgHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

            lastAnalysisTimestamp = currentTime
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = mutableListOf<OcrLine>()

                    visionText.textBlocks.forEach { block ->
                        block.lines.forEach { line ->
                            line.boundingBox?.let { box ->
                                val left = box.left.toFloat() / imgWidth
                                val right = box.right.toFloat() / imgWidth
                                val top = box.top.toFloat() / imgHeight
                                val bottom = box.bottom.toFloat() / imgHeight

                                val centerX = (left + right) / 2f
                                val centerY = (top + bottom) / 2f

                                val isHorizontallyIn = centerX in ROI_X_MIN..ROI_X_MAX
                                val isVerticallyIn = centerY in ROI_Y_MIN..ROI_Y_MAX

                                if (isHorizontallyIn && isVerticallyIn) {
                                    lines.add(
                                        OcrLine(
                                            text = line.text,
                                            width = box.width(),
                                            height = box.height(),
                                            top = box.top,
                                            left = box.left,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    onTextDetected(lines, imgWidth, imgHeight)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
