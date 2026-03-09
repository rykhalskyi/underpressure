package com.example.underpressure.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OcrParserTest {

    private val parser = OcrParser()

    @Test
    fun `test parse simple valid bp`() {
        val input = "120 80 72"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals(120, result?.systolic)
        assertEquals(80, result?.diastolic)
        assertEquals(72, result?.pulse)
    }

    @Test
    fun `test parse valid bp with text labels`() {
        val input = "SYS 128 mmHg DIA 84 mmHg Pulse 70 /min"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals(128, result?.systolic)
        assertEquals(84, result?.diastolic)
        assertEquals(70, result?.pulse)
    }

    @Test
    fun `test parse valid bp with multi-line input`() {
        val input = """
            135
            90
            65
        """.trimIndent()
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals(135, result?.systolic)
        assertEquals(90, result?.diastolic)
        assertEquals(65, result?.pulse)
    }

    @Test
    fun `test parse valid bp from noisy text`() {
        val input = "Some noisy data 08:30 AM SYS 115 DIA 75 PULSE 68 random text 123"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals(115, result?.systolic)
        assertEquals(75, result?.diastolic)
        assertEquals(68, result?.pulse)
    }

    @Test
    fun `test parse invalid values out of range`() {
        val input = "300 200 10" // SYS too high, DIA too high, Pulse too low
        val result = parser.parse(input)
        
        assertNull(result)
    }

    @Test
    fun `test parse insufficient numbers`() {
        val input = "120 80"
        val result = parser.parse(input)
        
        assertNull(result)
    }

    @Test
    fun `test parse empty string`() {
        val input = ""
        val result = parser.parse(input)
        
        assertNull(result)
    }

    @Test
    fun `test toFormattedString`() {
        val ocrResult = OcrResult(120, 80, 72)
        assertEquals("120/80 @72", ocrResult.toFormattedString())
    }
}
