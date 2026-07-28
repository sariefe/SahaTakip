package com.example.util

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class ScannedIdCardResult(
    val fullName: String,
    val tcNo: String,
    val serialNo: String = "A12B34567",
    val birthDate: String = "15.04.1992",
    val validUntil: String = "15.04.2032",
    val gender: String = "Belirtilmemiş",
    val confidenceScore: Float = 0.98f,
    val rawExtractedText: String = ""
)

data class IdCardPreset(
    val title: String,
    val fullName: String,
    val tcNo: String,
    val serialNo: String,
    val role: String
)

object OcrCardScanner {

    val availablePresets = listOf(
        IdCardPreset("Saha Şefi", "AHMET CAN YILMAZ", "10293847562", "A14X98231", "Saha Operasyon Şefi"),
        IdCardPreset("Saha Teknisyeni", "MEHMET ALİ DEMİR", "28374910284", "B22K48192", "Kıdemli Saha Teknisyeni"),
        IdCardPreset("Güvenlik Uzmanı", "AYŞE SULTAN KAYA", "39201847361", "C99M10293", "İSG & Güvenlik Uzmanı"),
        IdCardPreset("Sistem Operatörü", "ZEHRA ELİF ÇELİK", "48102938472", "D33P59102", "Sistem & Tesis Operatörü")
    )

    fun parseTextFromIdCard(rawText: String): ScannedIdCardResult? {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        // Regex for 11-digit TC No (more robust, might contain OCR errors like 0/O, 1/I)
        val cleanedText = rawText.replace(Regex("[OI]"), "0") // Basic OCR error correction
        val tcRegex = Regex("\\b[1-9][0-9]{10}\\b")
        val tcNo = tcRegex.find(rawText)?.value ?: tcRegex.find(cleanedText)?.value ?: return null

        var surname = ""
        var names = ""

        lines.forEachIndexed { index, line ->
            if (line.contains("SOYADI", ignoreCase = true) || line.contains("SURNAME", ignoreCase = true)) {
                surname = if (line.substringAfter(":", "").isNotBlank()) {
                    line.substringAfter(":").trim()
                } else if (index + 1 < lines.size) {
                    lines[index + 1].substringBefore(" ").trim()
                } else ""
            }
            if (line.contains("ADI", ignoreCase = true) || line.contains("GIVEN NAMES", ignoreCase = true)) {
                names = if (line.substringAfter(":", "").isNotBlank()) {
                    line.substringAfter(":").trim()
                } else if (index + 1 < lines.size) {
                    lines[index + 1].trim()
                } else ""
            }
        }

        val genderRaw = if (rawText.contains("CINSIYET", ignoreCase = true) || rawText.contains("GENDER", ignoreCase = true)) {
            val afterLabel = rawText.split(Regex("CINSIYET|GENDER", RegexOption.IGNORE_CASE)).lastOrNull()?.trim()
            afterLabel?.take(3)?.split("/")?.firstOrNull()?.trim() ?: ""
        } else ""

        val gender = when {
            genderRaw.startsWith("E", ignoreCase = true) || genderRaw.startsWith("M", ignoreCase = true) -> "Erkek"
            genderRaw.startsWith("K", ignoreCase = true) || genderRaw.startsWith("F", ignoreCase = true) -> "Kadın"
            else -> "Belirtilmemiş"
        }

        return ScannedIdCardResult(
            fullName = "${names.trim()} ${surname.trim()}".trim().ifBlank { "BİLİNMEYEN PERSONEL" },
            tcNo = tcNo,
            gender = gender,
            confidenceScore = 0.95f,
            rawExtractedText = rawText
        )
    }

    /**
     * Simulates scanning/OCR extraction from an ID Card image.
     */
    suspend fun processIdCardScan(
        fallbackNameInput: String = "",
        preset: IdCardPreset? = null
    ): ScannedIdCardResult {
        delay(1200.milliseconds) 

        if (preset != null) {
            val rawText = """
                TÜRKİYE CUMHURİYETİ KİMLİK KARTI / IDENTITY CARD
                TC KIMLIK NO / ID NO: ${preset.tcNo}
                SOYADI / SURNAME: ${preset.fullName.substringAfterLast(" ", "YILMAZ")}
                ADI / GIVEN NAMES: ${preset.fullName.substringBeforeLast(" ", "AHMET")}
                DOGUM TARIHI / DATE OF BIRTH: 12.08.1990
                SERI NO / DOCUMENT NO: ${preset.serialNo}
                SON GEÇERLİLİK / EXPIRY DATE: 12.08.2030
                CINSIYET / GENDER: E/M
                UYRUGU / NATIONALITY: T.C./TUR
                MRZ: I<TUR10293847562<<<<<<<<<<<<<<<9008124M3008122TUR<<<<<<<<<<<0
            """.trimIndent()

            return ScannedIdCardResult(
                fullName = preset.fullName,
                tcNo = preset.tcNo,
                serialNo = preset.serialNo,
                birthDate = "12.08.1990",
                validUntil = "12.08.2030",
                gender = if (preset.fullName.contains("AYŞE") || preset.fullName.contains("ZEHRA")) "Kadın" else "Erkek",
                confidenceScore = 0.985f,
                rawExtractedText = rawText
            )
        }

        if (fallbackNameInput.isNotBlank()) {
            val randomTc = (10000000000L..99999999999L).random().toString()
            val serial = "A${(10..99).random()}B${(10000..99999).random()}"
            val surname = fallbackNameInput.trim().substringAfterLast(" ", "CAN")
            val name = fallbackNameInput.trim().substringBeforeLast(" ", "AHMET")

            val rawText = """
                TÜRKİYE CUMHURİYETİ KİMLİK KARTI
                T.C. KN: $randomTc
                SOYADI: $surname
                ADI: $name
                SERI NO: $serial
                DOGUM TARIHI: 15.04.1992
                GEÇERLİLİK: 15.04.2032
                MRZ: I<TUR$randomTc<<<<<<<<<<<<<<<9204158M3204159TUR<<<<<<<<<<<4
            """.trimIndent()

            return ScannedIdCardResult(
                fullName = fallbackNameInput.trim().uppercase(java.util.Locale.forLanguageTag("tr")),
                tcNo = randomTc,
                serialNo = serial,
                birthDate = "15.04.1992",
                validUntil = "15.04.2032",
                gender = "Erkek", // Default for fallback
                confidenceScore = 0.978f,
                rawExtractedText = rawText
            )
        }

        val sample = availablePresets.random()
        return processIdCardScan(preset = sample)
    }
}

