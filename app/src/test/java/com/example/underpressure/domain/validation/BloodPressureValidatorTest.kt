package com.example.underpressure.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BloodPressureValidator.
 */
class BloodPressureValidatorTest {

    private lateinit var validator: BloodPressureValidator

    @Before
    fun setUp() {
        validator = BloodPressureValidator()
    }

    @Test
    fun `validate with valid input returns Success`() {
        val input = "120/80 @72"
        val result = validator.validate(input)
        
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(120, success.systolic)
        assertEquals(80, success.diastolic)
        assertEquals(72, success.pulse)
    }

    @Test
    fun `validate with valid input and different spaces returns Success`() {
        val input = "110/70@60"
        val result = validator.validate(input)
        
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(110, success.systolic)
        assertEquals(70, success.diastolic)
        assertEquals(60, success.pulse)
    }

    @Test
    fun `validate with empty input returns Error`() {
        val result = validator.validate("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Input cannot be empty", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validate with missing pulse returns Error`() {
        val result = validator.validate("120/80")
        assertTrue(result is ValidationResult.Error)
        assertEquals("Invalid format. Use: SYS/DIA @PULSE", (result as ValidationResult.Error).message)
    }

    @Test
    fun `validate with non-numeric values returns Error`() {
        val result = validator.validate("abc/80 @72")
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validate with one digit values returns Error`() {
        // Our regex \d{2,3} expects 2 to 3 digits
        val result = validator.validate("1/2 @3")
        assertTrue(result is ValidationResult.Error)
    }
    
    @Test
    fun `validate with too many digits returns Error`() {
        val result = validator.validate("1200/800 @720")
        assertTrue(result is ValidationResult.Error)
    }
}
