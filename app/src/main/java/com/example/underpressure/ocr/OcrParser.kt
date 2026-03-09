package com.example.underpressure.ocr

/**
 * Utility to parse systolic, diastolic and pulse values from raw OCR text.
 */
class OcrParser {

    /**
     * Parses the raw OCR text to extract blood pressure values.
     *
     * Strategies:
     * 1. Look for sequences of 2 or 3 digits.
     * 2. Identify SYS, DIA, and PULSE based on typical ranges:
     *    - SYS: 70-250
     *    - DIA: 40-150
     *    - PULSE: 40-200
     * 3. Look for labels like "SYS", "DIA", "PULSE" or "min" (for pulse).
     */
    fun parse(text: String): OcrResult? {
        if (text.isBlank()) return null

        // Find all numbers with 2 or 3 digits. 
        // We use a broader regex to find digits even if surrounded by some noise common in digital displays.
        val numberRegex = Regex("""(\d{2,3})""")
        val matches = numberRegex.findAll(text).map { it.value.toInt() }.toList()

        if (matches.size < 3) return null

        // Heuristic: Blood pressure monitors usually display SYS, DIA, and PULSE in a column (top to bottom).
        // They should appear in the text in this relative order.
        // We look for a triplet (i, j, k) where i < j < k and the values fit BP ranges.
        
        for (i in 0 until matches.size - 2) {
            val sys = matches[i]
            if (sys !in 70..250) continue
            
            for (j in i + 1 until matches.size - 1) {
                val dia = matches[j]
                if (dia !in 40..150 || dia >= sys) continue
                
                for (k in j + 1 until matches.size) {
                    val pulse = matches[k]
                    if (pulse in 30..220) {
                        return OcrResult(sys, dia, pulse)
                    }
                }
            }
        }

        return null
    }

    private fun isValidBp(sys: Int, dia: Int, pulse: Int): Boolean {
        return sys in 70..250 &&
               dia in 40..150 &&
               pulse in 30..220 &&
               sys > dia // Systolic must be greater than diastolic
    }
}
