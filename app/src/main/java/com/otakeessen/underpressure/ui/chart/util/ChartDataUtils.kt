package com.otakeessen.underpressure.ui.chart.util

import com.github.mikephil.charting.data.Entry
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.ui.chart.ChartMode
import com.otakeessen.underpressure.ui.chart.MeasurementType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

object ChartDataUtils {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateSequentialRollingAverage(
        values: List<Pair<Float, Float>>,
        windowSize: Int = 7
    ): List<Entry> {
        if (values.isEmpty() || windowSize <= 0) return emptyList()

        val effectiveWindow = max(windowSize, 1)
        var currentSum = 0f
        var windowStartIdx = 0

        return values.mapIndexed { index, (pos, value) ->
            currentSum += value

            if (index >= effectiveWindow) {
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
        if (dateValues.isEmpty() || windowDays <= 0) return emptyList()

        val sortedDates = dateValues.keys.sorted()
        val result = mutableListOf<Entry>()
        val effectiveWindow = max(windowDays, 1)

        var windowStartIdx = 0
        var currentSum = 0f
        var currentCount = 0

        for (currentDate in sortedDates) {
            val windowStartLimit = currentDate.minusDays((effectiveWindow - 1).toLong())

            for (value in dateValues[currentDate].orEmpty()) {
                currentSum += value
                currentCount++
            }

            while (windowStartIdx < sortedDates.size && sortedDates[windowStartIdx].isBefore(windowStartLimit)) {
                for (value in dateValues[sortedDates[windowStartIdx]].orEmpty()) {
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

    fun calculateRollingAverage(
        measurements: List<MeasurementEntity>,
        minDate: LocalDate,
        type: MeasurementType,
        mode: ChartMode
    ): List<Entry> {
        if (measurements.isEmpty()) return emptyList()

        return if (mode == ChartMode.TREND_BY_SLOT) {
            val sorted = measurements
                .filter { m -> type != MeasurementType.PULSE || m.pulse > 0 }
                .sortedBy { it.date }
            val valuesByDate = sorted.groupBy { LocalDate.parse(it.date, DATE_FORMATTER) }
                .mapValues { (_, ms) -> ms.map { m -> m.valueForType(type) } }
            calculateDailyRollingAverage(valuesByDate, minDate)
        } else {
            // For CHRONOLOGICAL mode, use the provided list as is (already sorted by time)
            val entries = measurements.mapIndexedNotNull { index, m ->
                val value = when (type) {
                    MeasurementType.SYS -> m.systolic.toFloat()
                    MeasurementType.DIA -> m.diastolic.toFloat()
                    MeasurementType.PULSE -> if (m.pulse > 0) m.pulse.toFloat() else null
                }
                value?.let { index.toFloat() to it }
            }
            
            calculateSequentialRollingAverage(entries)
        }
    }

    private fun MeasurementEntity.valueForType(type: MeasurementType): Float = when (type) {
        MeasurementType.SYS -> systolic.toFloat()
        MeasurementType.DIA -> diastolic.toFloat()
        MeasurementType.PULSE -> pulse.toFloat()
    }
}
