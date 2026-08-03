package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationTest {

    @Test
    fun `getTranslation returns Turkish text when language is tr`() {
        val result = getTranslation("Giriş", "Login", "tr")
        assertEquals("Giriş", result)
    }

    @Test
    fun `getTranslation returns English text when language is en`() {
        val result = getTranslation("Giriş", "Login", "en")
        assertEquals("Login", result)
    }

    @Test
    fun `getTranslation defaults to Turkish for unsupported language`() {
        val result = getTranslation("Giriş", "Login", "fr")
        assertEquals("Giriş", result)
    }
}
