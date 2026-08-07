package com.sahatakip.util

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrCleanerTest {

    @Test
    fun `cleanText with isNumeric=true should convert letters to numbers`() {
        val input = "O1L2ZS B$"
        val expected = "01122585"
        val result = OcrCleaner.cleanText(input, isNumeric = true)
        assertEquals(expected, result)
    }

    @Test
    fun `cleanText with isNumeric=false should convert numbers to letters and clean noise`() {
        val input = "AHM3T 1CAN|7$"
        // 3 -> (removed by regex), 1 -> I, | -> I, 7 -> (removed by regex), $ -> S
        val expected = "AHMT ICANIS" 
        val result = OcrCleaner.cleanText(input, isNumeric = false)
        assertEquals(expected, result)
    }

    @Test
    fun `fixTurkishCharacters should handle common OCR failures`() {
        val input = "S,ERIF C,AN G,OK O,Z U,NLER"
        val expected = "ŞERIF ÇAN ĞOK ÖZ ÜNLER"
        val result = OcrCleaner.cleanText(input, isNumeric = false)
        assertEquals(expected, result)
    }

    @Test
    fun `cleanText should handle lowercase inputs and convert to uppercase Turkish`() {
        val input = "ıiğüşöç"
        val expected = "IİĞÜŞÖÇ"
        val result = OcrCleaner.cleanText(input, isNumeric = false)
        assertEquals(expected, result)
    }

    @Test
    fun `cleanText should handle empty or blank strings`() {
        assertEquals("", OcrCleaner.cleanText(""))
        assertEquals("", OcrCleaner.cleanText("   "))
    }

    @Test
    fun `cleanText should remove all special characters except Turkish letters and space`() {
        val input = "AHMET !@# CAN"
        val expected = "AHMET  CAN" 
        val result = OcrCleaner.cleanText(input, isNumeric = false)
        assertEquals(expected, result)
    }

    @Test
    fun `cleanText numeric mode should keep minus sign but remove other symbols`() {
        val input = "-0505 123 45 67" 
        val expected = "-05051234567"
        val result = OcrCleaner.cleanText(input, isNumeric = true)
        assertEquals(expected, result)
    }
}
