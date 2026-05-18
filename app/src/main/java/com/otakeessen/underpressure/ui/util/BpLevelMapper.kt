package com.otakeessen.underpressure.ui.util

import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines

/**
 * Utility to map blood pressure levels to string resources based on guidelines.
 */
object BpLevelMapper {
    /**
     * Returns the string resource ID for a given blood pressure level and guidelines.
     */
    fun getStringRes(level: BloodPressureLevel, guidelines: BpGuidelines): Int {
        return when (level) {
            BloodPressureLevel.HYPOTENSION -> R.string.bp_level_hypotension
            BloodPressureLevel.NORMAL -> R.string.bp_level_normal
            BloodPressureLevel.ELEVATED -> if (guidelines == BpGuidelines.ESC_ESH) {
                R.string.bp_level_high_normal
            } else {
                R.string.bp_level_elevated
            }
            BloodPressureLevel.STAGE_1 -> if (guidelines == BpGuidelines.ESC_ESH) {
                R.string.bp_level_grade1
            } else {
                R.string.bp_level_stage1
            }
            BloodPressureLevel.STAGE_2 -> if (guidelines == BpGuidelines.ESC_ESH) {
                R.string.bp_level_grade2
            } else {
                R.string.bp_level_stage2
            }
            BloodPressureLevel.CRISIS -> R.string.bp_level_crisis
        }
    }

    /**
     * Returns the string resource ID for the range boundaries of a given blood pressure level and guidelines.
     */
    fun getRangeStringRes(level: BloodPressureLevel, guidelines: BpGuidelines): Int {
        return when (guidelines) {
            BpGuidelines.AHA_ACC -> when (level) {
                BloodPressureLevel.HYPOTENSION -> R.string.bp_range_aha_hypotension
                BloodPressureLevel.NORMAL -> R.string.bp_range_aha_normal
                BloodPressureLevel.ELEVATED -> R.string.bp_range_aha_elevated
                BloodPressureLevel.STAGE_1 -> R.string.bp_range_aha_stage1
                BloodPressureLevel.STAGE_2 -> R.string.bp_range_aha_stage2
                BloodPressureLevel.CRISIS -> R.string.bp_range_aha_crisis
            }
            BpGuidelines.ESC_ESH -> when (level) {
                BloodPressureLevel.HYPOTENSION -> R.string.bp_range_esc_hypotension
                BloodPressureLevel.NORMAL -> R.string.bp_range_esc_normal
                BloodPressureLevel.ELEVATED -> R.string.bp_range_esc_elevated
                BloodPressureLevel.STAGE_1 -> R.string.bp_range_esc_stage1
                BloodPressureLevel.STAGE_2 -> R.string.bp_range_esc_stage2
                BloodPressureLevel.CRISIS -> R.string.bp_range_esc_crisis
            }
        }
    }

    /**
     * Returns the string resource ID for the guideline source.
     */
    fun getSourceStringRes(guidelines: BpGuidelines): Int {
        return when (guidelines) {
            BpGuidelines.AHA_ACC -> R.string.label_source_aha
            BpGuidelines.ESC_ESH -> R.string.label_source_esc
        }
    }
}
