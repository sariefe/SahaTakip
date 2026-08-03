package com.example.util

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrAnalyzer(
    private val onTextDetected: (List<OcrLine>, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            
            // Image dimensions after rotation
            val imgWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
            val imgHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = mutableListOf<OcrLine>()
                    
                    // Region of Interest (ROI) - Central 90% width, 30% height
                    // Adjusted to better match the visual scanning box
                    val roiXMin = 0.05f
                    val roiXMax = 0.95f
                    val roiYMin = 0.35f
                    val roiYMax = 0.65f

                    visionText.textBlocks.forEach { block ->
                        block.lines.forEach { line ->
                            line.boundingBox?.let { box ->
                                // Calculate normalized boundaries
                                val left = box.left.toFloat() / imgWidth
                                val right = box.right.toFloat() / imgWidth
                                val top = box.top.toFloat() / imgHeight
                                val bottom = box.bottom.toFloat() / imgHeight

                                // Box-in-Frame check
                                val centerX = (left + right) / 2f
                                val centerY = (top + bottom) / 2f

                                val isHorizontallyIn = centerX in roiXMin..roiXMax
                                val isVerticallyIn = centerY in roiYMin..roiYMax

                                if (isHorizontallyIn && isVerticallyIn) {
                                    lines.add(
                                        OcrLine(
                                            text = line.text,
                                            height = box.height(),
                                            width = box.width(),
                                            top = box.top,
                                            left = box.left
                                        )
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
