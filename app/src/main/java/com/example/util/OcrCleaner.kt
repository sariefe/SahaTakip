package com.example.util

object OcrCleaner {

    /**
     * Cleans and normalizes text based on its expected type.
     */
    fun cleanText(text: String, isNumeric: Boolean = false): String {
        var cleaned = text.trim().uppercase(java.util.Locale.forLanguageTag("tr"))
        
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
                .replace(Regex("[^A-ZÇĞİÖŞÜ ]"), "")
        }
        
        return cleaned.trim()
    }

    /**
     * Attempts to fix broken Turkish characters often misread by standard Latin OCR
     */
    private fun fixTurkishCharacters(text: String): String {
        return text
            .replace("Ş", "Ş") // Ensure correct unicode
            .replace("Ğ", "Ğ")
            .replace("İ", "İ")
            // Heuristics for common failures
            .replace("S,", "Ş")
            .replace("C,", "Ç")
            .replace("G,", "Ğ")
            .replace("O,", "Ö")
            .replace("U,", "Ü")
    }

}
