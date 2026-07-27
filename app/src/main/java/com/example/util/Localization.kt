package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalLanguage = compositionLocalOf { "tr" }

@Composable
fun tr(trText: String, enText: String): String {
    return if (LocalLanguage.current == "en") enText else trText
}

