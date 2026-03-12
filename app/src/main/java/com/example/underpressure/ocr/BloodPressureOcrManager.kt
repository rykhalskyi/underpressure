package com.example.underpressure.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Manager for performing OCR on images to extract text.
 * Uses Google ML Kit Text Recognition.
 */
class BloodPressureOcrManager {

    private val mlKitRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val sevenSegmentRecognizer = SevenSegmentRecognizer()
    private val ocrParser = OcrParser()

    /**
     * Recognizes blood pressure from a bitmap image.
     * Attempts SevenSegmentRecognizer first, then falls back to ML Kit.
     *
     * @param bitmap The image to process.
     * @return The OcrResult, or null if recognition fails.
     */
    suspend fun recognize(bitmap: Bitmap): OcrResult? {
        // 1. Try SevenSegmentRecognizer first (optimized for digital displays)
        val sevenSegmentText = sevenSegmentRecognizer.recognize(bitmap)
        if (sevenSegmentText != null) {
            val result = ocrParser.parse(sevenSegmentText)
            if (result != null) return result
        }

        // 2. Fallback to ML Kit (optimized for printed text)
        val mlKitText = recognizeText(bitmap)
        if (mlKitText != null) {
            return ocrParser.parse(mlKitText)
        }

        return null
    }

    /**
     * Recognizes text from a bitmap image using ML Kit.
     *
     * @param bitmap The image to process.
     * @return The raw recognized text, or null if recognition fails.
     */
    private suspend fun recognizeText(bitmap: Bitmap): String? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = mlKitRecognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            null
        }
    }
}
