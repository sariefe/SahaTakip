package com.sahatakip.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

val LocalLanguage = compositionLocalOf { if (Locale.getDefault().language == "tr") "tr" else "en" }

@Composable
@ReadOnlyComposable
fun tr(trText: String, enText: String): String {
    return getTranslation(trText, enText, LocalLanguage.current)
}

fun trGlobal(trText: String, enText: String, language: String): String {
    return getTranslation(trText, enText, language)
}

fun getTranslation(trText: String, enText: String, language: String): String {
    return if (language == "en") enText else trText
}
