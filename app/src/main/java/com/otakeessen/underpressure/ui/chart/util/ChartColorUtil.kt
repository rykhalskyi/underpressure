package com.otakeessen.underpressure.ui.chart.util

import android.graphics.Color
import com.otakeessen.underpressure.domain.BloodPressureClassifier

object ChartColorUtil {
    private val DEFAULT_SLOT_COLORS = listOf(
        Color.BLUE,
        Color.GREEN,
        Color.parseColor("#FF9800"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#9C27B0") // Purple for Anytime
    )

    private val DEFAULT_LEVEL_COLORS = BloodPressureClassifier.getAllLevelColorsArgb()

    @JvmStatic
    fun getSlotColors(): List<Int> {
        return DEFAULT_SLOT_COLORS
    }

    @JvmStatic
    fun getLevelColors(): List<Int> {
        return DEFAULT_LEVEL_COLORS
    }
}
