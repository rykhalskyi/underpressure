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

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognizes text from a bitmap image.
     *
     * @param bitmap The image to process.
     * @return The raw recognized text, or null if recognition fails.
     */
    suspend fun recognizeText(bitmap: Bitmap): String? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            null
        }
    }
}
