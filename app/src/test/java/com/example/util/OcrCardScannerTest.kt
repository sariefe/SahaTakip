package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OcrCardScannerTest {

    @Test
    fun `parseStaffCardText handles positional layout correctly`() {
        // Mocking a typical card layout:
        val ocrLines = listOf(
            OcrLine("KURUM PERSONEL KARTI", 30, 200, 10, 50),
            OcrLine("ID-123456", 25, 100, 50, 50),
            OcrLine("MEHMET", 25, 80, 85, 50),
            OcrLine("AKSOY", 25, 80, 120, 50),
            OcrLine("TEKNIK BÖLÜMÜ", 20, 150, 155, 50)
        )

        val result = OcrCardScanner.parseStaffCardText(ocrLines)

        assertNotNull(result)
        assertEquals("1-123456", result?.staffId)
        assertEquals("MEHMET", result?.firstName)
        assertEquals("AKSOY", result?.lastName)
        assertEquals("TEKNIK BÖLÜMÜ", result?.department)
    }

    @Test
    fun `parseStaffCardText handles label-based layout correctly`() {
        val rawText = """
            PERSONEL KARTI
            İSİM: CAN
            SOYAD: ÖZTÜRK
            ID NO: 987654
            BÖLÜM: LOJİSTİK
        """.trimIndent()

        val result = OcrCardScanner.parseStaffCardText(rawText)

        assertNotNull(result)
        assertEquals("987654", result?.staffId)
        assertEquals("CAN", result?.firstName)
        assertEquals("ÖZTÜRK", result?.lastName)
        assertEquals("LOJİSTİK", result?.department)
    }

    @Test
    fun `parseStaffCardText handles noisy data and picks correct ID`() {
        val ocrLines = listOf(
            OcrLine("REPUBLIC OF TURKEY", 15, 100, 10, 50),
            OcrLine("SİSTEM GİRİŞ", 15, 100, 30, 50),
            OcrLine("555444", 25, 100, 60, 50), // This is the ID
            OcrLine("ZEYNEP", 25, 80, 95, 50),
            OcrLine("YILDIRIM", 25, 80, 130, 50)
        )

        val result = OcrCardScanner.parseStaffCardText(ocrLines)

        assertNotNull(result)
        assertEquals("555444", result?.staffId)
        assertEquals("ZEYNEP", result?.firstName)
        assertEquals("YILDIRIM", result?.lastName)
    }

    @Test
    fun `parseStaffCardText returns null for empty or irrelevant text`() {
        val result = OcrCardScanner.parseStaffCardText("HELLO WORLD\nNO RELEVANT DATA")
        // It might return a result if "NO" matches something, but generally should be null or blank fields
        // In OcrCardScanner: if (firstName.isBlank() && staffId.isBlank()) return null
        assertEquals(null, result)
    }
}
