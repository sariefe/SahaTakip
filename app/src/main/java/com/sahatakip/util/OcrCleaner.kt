package com.sahatakip.util

object OcrCleaner {

    /**
     * Cleans and normalizes text based on its expected type.
     */
    fun cleanText(text: String, isNumeric: Boolean = false): String {
        val trLocale = java.util.Locale.forLanguageTag("tr-TR")
        var cleaned = text.trim().uppercase(trLocale)
        
        // Fix Turkish specific misreads before general cleaning
        cleaned = fixTurkishCharacters(cleaned)

        cleaned = if (isNumeric) {
            cleaned.replace('O', '0')
                .replace('I', '1')
                .replace('L', '1')
                .replace('Z', '2')
                .replace('S', '5')
                .replace('B', '8')
                .replace('$', '5')
                .replace(Regex("[^0-9-]"), "")
        } else {
            cleaned.replace('0', 'O')
                .replace('1', 'I')
                .replace('5', 'S')
                .replace('8', 'B')
                // Common noise characters in names
                .replace('|', 'I')
                .replace('$', 'S')
                .replace(Regex("[^A-ZÇĞÖŞÜ\u0130 ]"), "")
        }
        
        return cleaned.trim()
    }

    /**
     * Attempts to fix broken Turkish characters often misread by standard Latin OCR
     */
    private fun fixTurkishCharacters(text: String): String {
        return text
            // Heuristics for common failures
            .replace("S,", "\u015E")
            .replace("C,", "\u00C7")
            .replace("G,", "\u011E")
            .replace("O,", "\u00D6")
            .replace("U,", "\u00DC")
            .replace("I,", "\u0049")
            .replace("i", "\u0130")
            .replace("\u0131", "\u0049")
    }

}
