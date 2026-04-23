package com.otakeessen.underpressure.ui.chart.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A Composable wrapper for MPAndroidChart's LineChart.
 *
 * @param lineData The data to be displayed in the chart.
 * @param startDate The reference start date for the X-axis (0-index).
 * @param modifier The modifier to be applied to the chart.
 * @param onChartReady Callback that provides a function to capture the chart as a bitmap.
 */
@Composable
fun BloodPressureChart(
    lineData: LineData?,
    startDate: LocalDate?,
    modifier: Modifier = Modifier,
    showXAxisLabels: Boolean = true,
    onChartReady: ((() -> Bitmap) -> Unit)? = null
) {

    // Get colors from the current Compose theme
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }


    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = true
                setPinchZoom(true)
                setDrawGridBackground(false)
                setDrawMarkers(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    setDrawLabels(showXAxisLabels)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                }

                axisRight.isEnabled = false

                legend.apply {
                    isEnabled = true
                    isWordWrapEnabled = true
                }

                onChartReady?.invoke {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(backgroundColor)
                    draw(canvas)
                    bitmap
                }
            }
        },
        update = { chart ->
            // Update marker
            chart.marker = BloodPressureMarkerView(chart, startDate)
            
            // Update colors to handle Dark/Light mode switching
            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.legend.textColor = textColor

            // Optional: Update grid line colors to match theme
            chart.axisLeft.gridColor = gridColor
            chart.xAxis.gridColor = gridColor

            chart.xAxis.setDrawLabels(showXAxisLabels)

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return startDate?.plusDays(value.toLong())?.format(dateFormatter) ?: value.toString()
                }
            }

            chart.data = lineData
            chart.invalidate()
        }
    )
}

