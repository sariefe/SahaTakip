package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalLanguage = compositionLocalOf { "tr" }

@Composable
fun tr(trText: String, enText: String): String {
    return getTranslation(trText, enText, LocalLanguage.current)
}

fun getTranslation(trText: String, enText: String, language: String): String {
    return if (language == "en") enText else trText
}
