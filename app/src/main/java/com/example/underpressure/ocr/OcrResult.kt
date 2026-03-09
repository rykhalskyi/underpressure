package com.example.underpressure.ocr

/**
 * Data class representing the result of blood pressure extraction from OCR text.
 *
 * @property systolic Systolic blood pressure value.
 * @property diastolic Diastolic blood pressure value.
 * @property pulse Pulse rate value.
 */
data class OcrResult(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int
) {
    /**
     * Formats the result as "SYS/DIA @PULSE".
     */
    fun toFormattedString(): String {
        return "$systolic/$diastolic @$pulse"
    }
}
