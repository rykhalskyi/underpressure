package com.otakeessen.underpressure.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.otakeessen.underpressure.domain.BloodPressureLevel

@Composable
fun BloodPressureLevel.getBackgroundColor(darkTheme: Boolean = isSystemInDarkTheme()): Color {
    return if (darkTheme) {
        when (this) {
            BloodPressureLevel.HYPOTENSION -> BgHypotensionDark
            BloodPressureLevel.NORMAL -> BgNormalDark
            BloodPressureLevel.ELEVATED -> BgElevatedDark
            BloodPressureLevel.STAGE_1 -> BgStage1Dark
            BloodPressureLevel.STAGE_2 -> BgStage2Dark
            BloodPressureLevel.CRISIS -> BgCrisisDark
        }
    } else {
        when (this) {
            BloodPressureLevel.HYPOTENSION -> BgHypotensionLight
            BloodPressureLevel.NORMAL -> BgNormalLight
            BloodPressureLevel.ELEVATED -> BgElevatedLight
            BloodPressureLevel.STAGE_1 -> BgStage1Light
            BloodPressureLevel.STAGE_2 -> BgStage2Light
            BloodPressureLevel.CRISIS -> BgCrisisLight
        }
    }
}

@Composable
fun BloodPressureLevel.getTextColor(): Color {
    return when (this) {
        BloodPressureLevel.HYPOTENSION -> ColorHypotension
        BloodPressureLevel.NORMAL -> ColorNormal
        BloodPressureLevel.ELEVATED -> ColorElevated
        BloodPressureLevel.STAGE_1 -> ColorStage1
        BloodPressureLevel.STAGE_2 -> ColorStage2
        BloodPressureLevel.CRISIS -> ColorCrisis
    }
}
