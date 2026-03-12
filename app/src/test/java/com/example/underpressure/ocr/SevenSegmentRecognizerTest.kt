package com.example.underpressure.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.osgi.OpenCVNativeLoader
import nu.pattern.OpenCV
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SevenSegmentRecognizerTest {

    private lateinit var recognizer: SevenSegmentRecognizer

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            OpenCV.loadShared()
        }
    }

    @Before
    fun setUp() {
        recognizer = SevenSegmentRecognizer()
    }

    @Test
    fun testCase1_7jpg() {
        val imagePath = "test_image/7.jpg"
        val bitmap = loadBitmap(imagePath)
        val result = recognizer.recognize(bitmap)
        
        // Expected output: Row 1: 123, Row 2: 81, Row 3: 92
        assertEquals("123\n81\n92", result)
    }

    @Test
    fun testCase2_8jpg() {
        val imagePath = "test_image/8.jpg"
        val bitmap = loadBitmap(imagePath)
        val result = recognizer.recognize(bitmap)
        
        // Expected output: Row 1: 141, Row 2: 90, Row 3: 85
        assertEquals("141\n90\n85", result)
    }

    private fun loadBitmap(path: String): Bitmap {
        val file = File(path)
        if (!file.exists()) {
            // Try with project root relative path if needed, 
            // but usually tests run from project root or module root.
            throw IllegalArgumentException("File not found: ${file.absolutePath}")
        }
        return BitmapFactory.decodeFile(file.absolutePath)
    }
}
