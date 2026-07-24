package com.example.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource

val LocalLanguage = compositionLocalOf { "tr" }

@Composable
fun tr(trText: String, enText: String): String {
    return if (LocalLanguage.current == "en") enText else trText
}

@Composable
fun strRes(@StringRes id: Int, fallbackTr: String = "", fallbackEn: String = ""): String {
    val resString = stringResource(id = id)
    return if (resString.isNotEmpty()) {
        resString
    } else {
        if (LocalLanguage.current == "en") fallbackEn else fallbackTr
    }
}

fun getLocalizedString(trText: String, enText: String, lang: String): String {
    return if (lang == "en") enText else trText
}
