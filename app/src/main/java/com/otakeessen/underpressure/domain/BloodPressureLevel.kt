package com.otakeessen.underpressure.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Represents different levels of blood pressure according to official classification.
 */
enum class BloodPressureLevel {
    HYPOTENSION,
    NORMAL,
    ELEVATED,
    STAGE_1,
    STAGE_2,
    CRISIS
}

/**
 * Supported blood pressure classification guidelines.
 */
enum class BpGuidelines {
    AHA_ACC, // American
    ESC_ESH  // European
}

/**
 * Data class representing the result of a blood pressure classification.
 */
data class ClassificationResult(
    val level: BloodPressureLevel,
    val textColor: Color,
    val backgroundColor: Color,
    val isBold: Boolean
)

/**
 * Classifies blood pressure based on systolic and diastolic values using strategies.
 */
object BloodPressureClassifier {
    
    // Centralized thresholds
    private const val AHA_CRISIS_SYS = 180
    private const val AHA_CRISIS_DIA = 120
    private const val AHA_STAGE2_SYS = 140
    private const val AHA_STAGE2_DIA = 90
    private const val AHA_STAGE1_SYS = 130
    private const val AHA_STAGE1_DIA = 80
    private const val AHA_ELEVATED_SYS = 120
    private const val AHA_ELEVATED_DIA_MAX = 80

    private const val ESC_CRISIS_SYS = 180
    private const val ESC_CRISIS_DIA = 110
    private const val ESC_STAGE2_SYS = 160
    private const val ESC_STAGE2_DIA = 100
    private const val ESC_STAGE1_SYS = 140
    private const val ESC_STAGE1_DIA = 90
    private const val ESC_ELEVATED_SYS = 130
    private const val ESC_ELEVATED_DIA = 85

    private const val HYPOTENSION_SYS = 90
    private const val HYPOTENSION_DIA = 60

    // Centralized colors
    val COLOR_HYPOTENSION = Color(0xFF1976D2) // Blue
    val COLOR_NORMAL = Color(0xFF2E7D32)   // Green
    val COLOR_ELEVATED = Color(0xFFE6AC00) // Amber
    val COLOR_STAGE_1 = Color(0xFFE67E22)  // Orange
    val COLOR_STAGE_2 = Color(0xFFC0392B)  // Red
    val COLOR_CRISIS = Color(0xFF8B0000)   // Dark Red

    private val BG_HYPOTENSION = Color(0xFFE3F2FD) // Very Light Blue
    private val BG_NORMAL = Color(0xFFE8F5E9)      // Very Light Green
    private val BG_ELEVATED = Color(0xFFFFF9C4)    // Very Light Yellow
    private val BG_STAGE_1 = Color(0xFFFFE0B2)    // Very Light Orange
    private val BG_STAGE_2 = Color(0xFFFFCDD2)    // Very Light Red
    private val BG_CRISIS = Color(0xFFFFBDBB)     // Light Red

    /**
     * Returns all level colors as ARGB integers for use in non-Compose contexts (like charts).
     */
    fun getAllLevelColorsArgb(): List<Int> {
        return listOf(
            COLOR_HYPOTENSION,
            COLOR_NORMAL,
            COLOR_ELEVATED,
            COLOR_STAGE_1,
            COLOR_STAGE_2,
            COLOR_CRISIS
        ).map { it.toArgb() }
    }

    fun classify(systolic: Int, diastolic: Int, guidelines: BpGuidelines = BpGuidelines.ESC_ESH): ClassificationResult {
        val level = when (guidelines) {
            BpGuidelines.AHA_ACC -> classifyAmerican(systolic, diastolic)
            BpGuidelines.ESC_ESH -> classifyEuropean(systolic, diastolic)
        }

        return ClassificationResult(
            level = level,
            textColor = level.toTextColor(),
            backgroundColor = level.toBackgroundColor(),
            isBold = false//level >= BloodPressureLevel.STAGE_1
        )
    }

    private fun classifyAmerican(systolic: Int, diastolic: Int): BloodPressureLevel {
        return when {
            systolic >= AHA_CRISIS_SYS || diastolic >= AHA_CRISIS_DIA -> BloodPressureLevel.CRISIS
            systolic >= AHA_STAGE2_SYS || diastolic >= AHA_STAGE2_DIA -> BloodPressureLevel.STAGE_2
            systolic >= AHA_STAGE1_SYS || diastolic >= AHA_STAGE1_DIA -> BloodPressureLevel.STAGE_1
            systolic >= AHA_ELEVATED_SYS && diastolic < AHA_ELEVATED_DIA_MAX -> BloodPressureLevel.ELEVATED
            systolic < HYPOTENSION_SYS || diastolic < HYPOTENSION_DIA -> BloodPressureLevel.HYPOTENSION
            else -> BloodPressureLevel.NORMAL
        }
    }

    private fun classifyEuropean(systolic: Int, diastolic: Int): BloodPressureLevel {
        return when {
            systolic >= ESC_CRISIS_SYS || diastolic >= ESC_CRISIS_DIA -> BloodPressureLevel.CRISIS
            systolic >= ESC_STAGE2_SYS || diastolic >= ESC_STAGE2_DIA -> BloodPressureLevel.STAGE_2 // Grade 2
            systolic >= ESC_STAGE1_SYS || diastolic >= ESC_STAGE1_DIA -> BloodPressureLevel.STAGE_1  // Grade 1
            systolic >= ESC_ELEVATED_SYS || diastolic >= ESC_ELEVATED_DIA -> BloodPressureLevel.ELEVATED // High Normal
            systolic < HYPOTENSION_SYS || diastolic < HYPOTENSION_DIA -> BloodPressureLevel.HYPOTENSION
            else -> BloodPressureLevel.NORMAL
        }
    }

    private fun BloodPressureLevel.toTextColor(): Color = when (this) {
        BloodPressureLevel.HYPOTENSION -> COLOR_HYPOTENSION
        BloodPressureLevel.NORMAL -> COLOR_NORMAL
        BloodPressureLevel.ELEVATED -> COLOR_ELEVATED
        BloodPressureLevel.STAGE_1 -> COLOR_STAGE_1
        BloodPressureLevel.STAGE_2 -> COLOR_STAGE_2
        BloodPressureLevel.CRISIS -> COLOR_CRISIS
    }

    private fun BloodPressureLevel.toBackgroundColor(): Color = when (this) {
        BloodPressureLevel.HYPOTENSION -> BG_HYPOTENSION
        BloodPressureLevel.NORMAL -> BG_NORMAL
        BloodPressureLevel.ELEVATED -> BG_ELEVATED
        BloodPressureLevel.STAGE_1 -> BG_STAGE_1
        BloodPressureLevel.STAGE_2 -> BG_STAGE_2
        BloodPressureLevel.CRISIS -> BG_CRISIS
    }
    
    /**
     * Extension to map level to color (for summary components)
     */
    fun BloodPressureLevel.toColor(): Color = this.toTextColor()
}
