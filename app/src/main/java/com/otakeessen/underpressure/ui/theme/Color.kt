package com.otakeessen.underpressure.ui.theme

import androidx.compose.ui.graphics.Color
import com.otakeessen.underpressure.domain.BloodPressureClassifier

// Primary/Secondary Palette
val PrimaryLight = Color(0xFF613DC1)
val OnPrimaryLight = Color(0xFFFFFFFF)
val SecondaryLight = Color(0xFF8497F6)
val TertiaryLight = Color(0xFFF7AEF8)

val PrimaryDark = Color(0xFFC399F9)
val OnPrimaryDark = Color(0xFF381E72)
val SecondaryDark = Color(0xFF8497F6)
val TertiaryDark = Color(0xFFF7AEF8)

// Status Colors (Centralized in BloodPressureLevel.kt)
val ColorHypotension = BloodPressureClassifier.COLOR_HYPOTENSION
val ColorNormal = BloodPressureClassifier.COLOR_NORMAL
val ColorElevated = BloodPressureClassifier.COLOR_ELEVATED
val ColorStage1 = BloodPressureClassifier.COLOR_STAGE_1
val ColorStage2 = BloodPressureClassifier.COLOR_STAGE_2
val ColorCrisis = BloodPressureClassifier.COLOR_CRISIS

// Status Backgrounds (Light Mode - Centralized in BloodPressureLevel.kt)
val BgHypotensionLight = BloodPressureClassifier.BG_HYPOTENSION
val BgNormalLight = BloodPressureClassifier.BG_NORMAL
val BgElevatedLight = BloodPressureClassifier.BG_ELEVATED
val BgStage1Light = BloodPressureClassifier.BG_STAGE_1
val BgStage2Light = BloodPressureClassifier.BG_STAGE_2
val BgCrisisLight = BloodPressureClassifier.BG_CRISIS

// Status Backgrounds (Dark Mode - Subtle Tints)
val BgHypotensionDark = Color(0xFF2B3252)
val BgNormalDark = Color(0xFF333252)
val BgElevatedDark = Color(0xFF3B3252)
val BgStage1Dark = Color(0xFF423552)
val BgStage2Dark = Color(0xFF523752)
val BgCrisisDark = Color(0xFF52223E)
