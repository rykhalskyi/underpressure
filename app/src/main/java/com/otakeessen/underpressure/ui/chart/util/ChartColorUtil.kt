package com.otakeessen.underpressure.ui.chart.util

import android.graphics.Color

object ChartColorUtil {
    private val DEFAULT_SLOT_COLORS = listOf(
        Color.BLUE,
        Color.GREEN,
        Color.parseColor("#FF9800"),
        Color.parseColor("#E91E63")
    )

    private val DEFAULT_LEVEL_COLORS = listOf(
        Color.parseColor("#2E7D32"),
        Color.parseColor("#E6AC00"),
        Color.parseColor("#E67E22"),
        Color.parseColor("#C0392B"),
        Color.parseColor("#8B0000")
    )

    @JvmStatic
    fun getSlotColors(): List<Int> {
        return DEFAULT_SLOT_COLORS
    }

    @JvmStatic
    fun getLevelColors(): List<Int> {
        return DEFAULT_LEVEL_COLORS
    }
}
