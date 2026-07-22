package com.example.util

import android.graphics.Bitmap
import kotlinx.coroutines.delay

data class ScannedIdCardResult(
    val fullName: String,
    val tcNo: String,
    val confidenceScore: Float,
    val rawExtractedText: String
)

object OcrCardScanner {

    private val sampleNames = listOf(
        Pair("AHMET CAN YILMAZ", "10293847562"),
        Pair("MEHMET ALİ DEMİR", "28374910284"),
        Pair("AYŞE SULTAN KAYA", "39201847361"),
        Pair("ZEHRA ELİF ÇELİK", "48102938472"),
        Pair("MURAT ŞAHİN", "57201938461")
    )

    /**
     * Simulates scanning/OCR extraction from an ID Card image.
     */
    suspend fun processIdCardScan(bitmap: Bitmap? = null, fallbackNameInput: String = ""): ScannedIdCardResult {
        delay(1500) // Simulate OCR image recognition latency
        if (fallbackNameInput.isNotBlank()) {
            val randomTc = (10000000000L..99999999999L).random().toString()
            return ScannedIdCardResult(
                fullName = fallbackNameInput.trim().uppercase(),
                tcNo = randomTc,
                confidenceScore = 0.98f,
                rawExtractedText = "T.C. KİMLİK KARTI\nSOYADI: ${fallbackNameInput.substringAfterLast(" ", "")}\nADI: ${fallbackNameInput.substringBeforeLast(" ", "")}\nTC NO: $randomTc"
            )
        }

        val sample = sampleNames.random()
        return ScannedIdCardResult(
            fullName = sample.first,
            tcNo = sample.second,
            confidenceScore = 0.96f,
            rawExtractedText = "TÜRKİYE CUMHURİYETİ KİMLİK KARTI\nSURNAME: ${sample.first.substringAfterLast(" ")}\nGIVEN NAME: ${sample.first.substringBeforeLast(" ")}\nDOCUMENT NO: T11B9${(1000..9999).random()}\nTC: ${sample.second}"
        )
    }
}
