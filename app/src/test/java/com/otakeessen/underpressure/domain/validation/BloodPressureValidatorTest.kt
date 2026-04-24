package com.otakeessen.underpressure.domain.validation

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
    fun `validate with original format returns Success`() {
        val input = "120/80 @72"
        val result = validator.validate(input)
        
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(120, success.systolic)
        assertEquals(80, success.diastolic)
        assertEquals(72, success.pulse)
    }

    @Test
    fun `validate with space delimiter returns Success`() {
        val input = "120 80 72"
        val result = validator.validate(input)
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(120, success.systolic)
        assertEquals(80, success.diastolic)
        assertEquals(72, success.pulse)
    }

    @Test
    fun `validate with comma delimiter returns Success`() {
        val input = "120,80,72"
        val result = validator.validate(input)
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(120, success.systolic)
        assertEquals(80, success.diastolic)
        assertEquals(72, success.pulse)
    }

    @Test
    fun `validate with multiple slashes returns Success`() {
        val input = "120/80/72"
        val result = validator.validate(input)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validate with missing pulse returns Success with pulse 0`() {
        val input = "120/80"
        val result = validator.validate(input)
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(120, success.systolic)
        assertEquals(80, success.diastolic)
        assertEquals(0, success.pulse)
    }

    @Test
    fun `validate with spaces and missing pulse returns Success`() {
        val input = "120 80"
        val result = validator.validate(input)
        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals(120, success.systolic)
        assertEquals(80, success.diastolic)
        assertEquals(0, success.pulse)
    }

    @Test
    fun `validate with empty input returns EmptyInput Error`() {
        val result = validator.validate("")
        assertTrue(result is ValidationResult.Error.EmptyInput)
    }

    @Test
    fun `validate with invalid format returns InvalidFormat Error`() {
        val result = validator.validate("120")
        assertTrue(result is ValidationResult.Error.InvalidFormat)
    }

    @Test
    fun `validate with non-numeric values returns InvalidFormat Error`() {
        val result = validator.validate("abc/80 @72")
        assertTrue(result is ValidationResult.Error.InvalidFormat)
    }

    @Test
    fun `validate with sys less than dia returns IncorrectMeasurements Error`() {
        val result = validator.validate("80/120")
        assertTrue(result is ValidationResult.Error.IncorrectMeasurements)
    }

    @Test
    fun `validate with sys out of range returns IncorrectMeasurements Error`() {
        val result = validator.validate("30/20")
        assertTrue(result is ValidationResult.Error.IncorrectMeasurements)
    }

    @Test
    fun `validate with dia out of range returns IncorrectMeasurements Error`() {
        val result = validator.validate("120/10")
        assertTrue(result is ValidationResult.Error.IncorrectMeasurements)
    }

    @Test
    fun `validate with pulse out of range returns IncorrectMeasurements Error`() {
        val result = validator.validate("120/80 20")
        assertTrue(result is ValidationResult.Error.IncorrectMeasurements)
    }
}
