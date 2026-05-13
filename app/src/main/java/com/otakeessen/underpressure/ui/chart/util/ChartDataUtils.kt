package com.otakeessen.underpressure.ui.chart.util

import com.github.mikephil.charting.data.Entry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ChartDataUtils {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateSequentialRollingAverage(
        values: List<Pair<Float, Float>>,
        windowSize: Int = 7
    ): List<Entry> {
        if (values.isEmpty()) return emptyList()

        var currentSum = 0f
        var windowStartIdx = 0

        return values.mapIndexed { index, (pos, value) ->
            currentSum += value

            if (index >= windowSize) {
                currentSum -= values[windowStartIdx].second
                windowStartIdx++
            }

            val count = index - windowStartIdx + 1
            Entry(pos, currentSum / count)
        }
    }

    fun calculateDailyRollingAverage(
        dateValues: Map<LocalDate, List<Float>>,
        minDate: LocalDate,
        windowDays: Int = 7
    ): List<Entry> {
        if (dateValues.isEmpty()) return emptyList()

        val sortedDates = dateValues.keys.sorted()
        val result = mutableListOf<Entry>()

        var windowStartIdx = 0
        var currentSum = 0f
        var currentCount = 0

        for (currentDate in sortedDates) {
            val windowStartLimit = currentDate.minusDays((windowDays - 1).toLong())

            val valuesForDate = dateValues[currentDate] ?: emptyList()
            for (value in valuesForDate) {
                currentSum += value
                currentCount++
            }

            while (windowStartIdx < sortedDates.size && sortedDates[windowStartIdx].isBefore(windowStartLimit)) {
                val oldDate = sortedDates[windowStartIdx]
                val oldValues = dateValues[oldDate] ?: emptyList()
                for (value in oldValues) {
                    currentSum -= value
                    currentCount--
                }
                windowStartIdx++
            }

            if (currentCount > 0) {
                val x = ChronoUnit.DAYS.between(minDate, currentDate).toFloat()
                result.add(Entry(x, currentSum / currentCount))
            }
        }

        return result
    }
}
