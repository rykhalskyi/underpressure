package com.otakeessen.underpressure.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BloodPressureClassifierTest {

    @Test
    fun `classify American - Normal`() {
        val result = BloodPressureClassifier.classify(110, 70, BpGuidelines.AHA_ACC)
        assertEquals(BloodPressureLevel.NORMAL, result.level)
    }

    @Test
    fun `classify American - Elevated`() {
        val result = BloodPressureClassifier.classify(125, 75, BpGuidelines.AHA_ACC)
        assertEquals(BloodPressureLevel.ELEVATED, result.level)
    }

    @Test
    fun `classify American - Stage 1`() {
        val result = BloodPressureClassifier.classify(130, 80, BpGuidelines.AHA_ACC)
        assertEquals(BloodPressureLevel.STAGE_1, result.level)
    }

    @Test
    fun `classify American - Stage 2`() {
        val result = BloodPressureClassifier.classify(145, 95, BpGuidelines.AHA_ACC)
        assertEquals(BloodPressureLevel.STAGE_2, result.level)
    }

    @Test
    fun `classify American - Crisis`() {
        val result = BloodPressureClassifier.classify(180, 120, BpGuidelines.AHA_ACC)
        assertEquals(BloodPressureLevel.CRISIS, result.level)
    }

    @Test
    fun `classify European - Normal`() {
        val result = BloodPressureClassifier.classify(110, 70, BpGuidelines.ESC_ESH)
        assertEquals(BloodPressureLevel.NORMAL, result.level)
    }

    @Test
    fun `classify European - Elevated (High Normal)`() {
        val result = BloodPressureClassifier.classify(135, 88, BpGuidelines.ESC_ESH)
        assertEquals(BloodPressureLevel.ELEVATED, result.level)
    }

    @Test
    fun `classify European - Stage 1 (Grade 1)`() {
        val result = BloodPressureClassifier.classify(145, 95, BpGuidelines.ESC_ESH)
        assertEquals(BloodPressureLevel.STAGE_1, result.level)
    }

    @Test
    fun `classify European - Stage 2 (Grade 2)`() {
        val result = BloodPressureClassifier.classify(165, 105, BpGuidelines.ESC_ESH)
        assertEquals(BloodPressureLevel.STAGE_2, result.level)
    }

    @Test
    fun `classify European - Crisis (Grade 3 or Crisis)`() {
        val result = BloodPressureClassifier.classify(180, 110, BpGuidelines.ESC_ESH)
        assertEquals(BloodPressureLevel.CRISIS, result.level)
    }
}
