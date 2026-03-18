package com.example.underpressure.ui.chart.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData

/**
 * A Composable wrapper for MPAndroidChart's LineChart.
 *
 * @param lineData The data to be displayed in the chart.
 * @param modifier The modifier to be applied to the chart.
 * @param onChartReady Callback that provides a function to capture the chart as a bitmap.
 */
@Composable
fun BloodPressureChart(
    lineData: LineData?,
    modifier: Modifier = Modifier,
    onChartReady: ((() -> Bitmap) -> Unit)? = null
) {

    // Get colors from the current Compose theme
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()


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

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
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
                    draw(canvas)
                    bitmap
                }
            }
        },
        update = { chart ->
            // Update colors to handle Dark/Light mode switching
            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.legend.textColor = textColor

            // Optional: Update grid line colors to match theme
            chart.axisLeft.gridColor = gridColor
            chart.xAxis.gridColor = gridColor

            chart.data = lineData
            chart.invalidate()
        }
    )
}
