package com.example.util

import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class ScannedStaffCardResult(
    val firstName: String,
    val lastName: String,
    val staffId: String,
    val department: String,
    val confidenceScore: Float = 0.98f,
    val rawExtractedText: String = ""
) {
    val fullName: String get() = "$firstName $lastName"
}

data class StaffCardPreset(
    val title: String,
    val firstName: String,
    val lastName: String,
    val staffId: String,
    val department: String,
    val role: String
)

object OcrCardScanner {

    private val DEPARTMENTS = listOf("SAHA", "TEKNİK", "GÜVENLİK", "LOJİSTİK", "YÖNETİM", "BİLGİ İŞLEM")

    val availablePresets = listOf(
        StaffCardPreset("Saha Şefi", "AHMET CAN", "YILMAZ", "ID-2026-001", "SAHA", "Saha Operasyon Şefi"),
        StaffCardPreset("Saha Teknisyeni", "MEHMET ALİ", "DEMİR", "ID-2026-142", "TEKNİK", "Kıdemli Saha Teknisyeni"),
        StaffCardPreset("Güvenlik Uzmanı", "AYŞE SULTAN", "KAYA", "ID-2026-088", "GÜVENLİK", "İSG & Güvenlik Uzmanı"),
        StaffCardPreset("Sistem Operatörü", "ZEHRA ELİF", "ÇELİK", "ID-2026-305", "YÖNETİM", "Sistem & Tesis Operatörü")
    )

    fun parseStaffCardText(ocrLines: List<OcrLine>): ScannedStaffCardResult? {
        if (ocrLines.isEmpty()) return null

        val upperLines = ocrLines.map { it.copy(text = it.text.uppercase(Locale.forLanguageTag("tr"))) }
        
        val avgHeight = ocrLines.map { it.height }.average()
        
        var staffId = ""
        var firstName = ""
        var lastName = ""
        var department = ""
        
        val idRegex = Regex("\\b(ID-)?\\d{3,10}\\b")
        val noiseHeaders = listOf(
            "KARTI", "PERSONEL", "KURUM", "SİSTEM", "TAKİP", "HOŞGELDİNİZ", 
            "T.C.", "KİMLİK", "REPUBLİC", "TURKEY", "CARD", "IDENTITY"
        )
        val deptKeywords = listOf("MÜDÜRLÜĞÜ", "MÜDÜRLÜK", "DEPARTMANI", "BÖLÜMÜ", "BİRİMİ", "AMİRLİĞİ")

        // 1. ANCHOR SEARCH: Find the ID line first regardless of position
        // Only look at lines that don't contain noise headers and are large enough
        val potentialIdLines = upperLines.filter { line ->
            !noiseHeaders.any { line.text.contains(it) } && 
            (line.height > avgHeight * 0.85) // Slightly more relaxed height constraint
        }

        val idLine = potentialIdLines.find { idRegex.containsMatchIn(it.text) }
            ?: upperLines.find { idRegex.containsMatchIn(it.text) } // Fallback to any line

        if (idLine != null) {
            staffId = OcrCleaner.cleanText(idRegex.find(idLine.text)?.value ?: idLine.text, isNumeric = true)
            
            // 2. RELATIVE SEARCH: Look for Name, Surname and Dept BELOW the ID line
            // Only consider lines that are below the ID line vertically
            val linesBelowId = upperLines.filter { it.top > idLine.top + (idLine.height / 2) }
                .filter { !noiseHeaders.any { noise -> it.text.contains(noise) } }
                .sortedBy { it.top }

            if (linesBelowId.isNotEmpty()) {
                // Heuristic: First line after ID is Name
                firstName = OcrCleaner.cleanText(linesBelowId[0].text, isNumeric = false)
                
                if (linesBelowId.size >= 2) {
                    // Second line after ID is Surname
                    lastName = OcrCleaner.cleanText(linesBelowId[1].text, isNumeric = false)
                }
                
                // Search all lines below ID for Department (not just the 3rd line)
                val deptLine = linesBelowId.drop(1).find { candidate ->
                    deptKeywords.any { candidate.text.contains(it) } || 
                    DEPARTMENTS.any { candidate.text.contains(it) }
                }
                if (deptLine != null) {
                    department = OcrCleaner.cleanText(deptLine.text, isNumeric = false)
                } else if (linesBelowId.size >= 3 && department.isBlank()) {
                    department = OcrCleaner.cleanText(linesBelowId[2].text, isNumeric = false)
                }
            }
        }

        // 3. FALLBACK: If positional search failed, try old label-based search
        if (firstName.isBlank() || staffId.isBlank()) {
            upperLines.forEachIndexed { index, line ->
                val text = line.text
                when {
                    (text.contains("ID") || text.contains("NO")) && staffId.isBlank() -> {
                        staffId = OcrCleaner.cleanText(extractValue(text) ?: if (index + 1 < upperLines.size) upperLines[index + 1].text else "", isNumeric = true)
                    }
                    (text.contains("İSİM") || text.contains("ADI") || text.contains("NAME")) && firstName.isBlank() -> {
                        firstName = OcrCleaner.cleanText(extractValue(text) ?: if (index + 1 < upperLines.size) upperLines[index + 1].text else "", isNumeric = false)
                    }
                    (text.contains("SOYAD") || text.contains("SURNAME")) && lastName.isBlank() -> {
                        lastName = OcrCleaner.cleanText(extractValue(text) ?: if (index + 1 < upperLines.size) upperLines[index + 1].text else "", isNumeric = false)
                    }
                    (text.contains("DEPARTMAN") || text.contains("BÖLÜM") || text.contains("MÜDÜRLÜK")) && department.isBlank() -> {
                        val rawValue = extractValue(text) ?: if (index + 1 < upperLines.size) upperLines[index + 1].text else ""
                        department = OcrCleaner.cleanText(rawValue, isNumeric = false)
                    }
                }
            }
        }

        // 4. Department match refinement (if still blank)
        if (department.isBlank()) {
            department = DEPARTMENTS.find { dept -> upperLines.any { it.text.contains(dept) } } 
                ?: upperLines.find { line -> deptKeywords.any { line.text.contains(it) } }?.let { OcrCleaner.cleanText(it.text, isNumeric = false) }
                ?: ""
        }

        if (firstName.isBlank() && staffId.isBlank()) return null

        val foundFieldsCount = listOf(staffId, firstName, lastName, department).count { it.isNotBlank() }
        val confidence = 0.85f + (foundFieldsCount * 0.03f)

        return ScannedStaffCardResult(
            firstName = firstName.trim().uppercase(Locale.forLanguageTag("tr")),
            lastName = lastName.trim().uppercase(Locale.forLanguageTag("tr")),
            staffId = staffId.trim(),
            department = department.trim().uppercase(Locale.forLanguageTag("tr")),
            confidenceScore = confidence.coerceAtMost(0.99f),
            rawExtractedText = ocrLines.joinToString("\n") { it.text }
        )
    }

    fun parseStaffCardText(rawText: String): ScannedStaffCardResult? {
        val lines = rawText.lines().map { OcrLine(it, 20, 0, 0, 0) }
        return parseStaffCardText(lines)
    }

    private fun extractValue(line: String): String? {
        val parts = line.split(":")
        return if (parts.size > 1 && parts[1].trim().isNotBlank()) parts[1].trim() else null
    }

    suspend fun processStaffCardScan(
        preset: StaffCardPreset? = null
    ): ScannedStaffCardResult {
        delay(1200.milliseconds) 

        if (preset != null) {
            val rawText = """
                KURUM PERSONEL KARTI
                İSİM: ${preset.firstName}
                SOYİSİM: ${preset.lastName}
                DEPARTMAN: ${preset.department}
                PERSONEL ID: ${preset.staffId}
            """.trimIndent()

            return scannedStaffStaffCardResult(preset, rawText)
        }

        // Simulate a "No Keyword" card for fallback
        val randomId = (10000..99999).random().toString()
        val randomDept = DEPARTMENTS.random()
        val rawNoLabels = """
            PERSONEL TAKİP SİSTEMİ
            MUSTAFA
            ÖZTÜRK
            $randomDept
            $randomId
        """.trimIndent()

        return parseStaffCardText(rawNoLabels) ?: ScannedStaffCardResult("BİLİNMEYEN", "PERSONEL", randomId, "SAHA")
    }

    private fun scannedStaffStaffCardResult(preset: StaffCardPreset, rawText: String) = ScannedStaffCardResult(
        firstName = preset.firstName,
        lastName = preset.lastName,
        staffId = preset.staffId,
        department = preset.department,
        confidenceScore = 0.99f,
        rawExtractedText = rawText
    )
}

