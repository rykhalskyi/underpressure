package com.otakeessen.underpressure.ui.chart.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.formatter.ValueFormatter
import com.otakeessen.underpressure.domain.BloodPressureLevel

@Composable
fun BloodPressureBarChart(
    barData: BarData?,
    xLabels: Map<Float, String> = emptyMap(),
    modifier: Modifier = Modifier,
    onChartReady: ((() -> Bitmap) -> Unit)? = null
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                BarChart(context).apply {
                    description.isEnabled = false
                    setTouchEnabled(true)
                    isDragEnabled = true
                    setScaleEnabled(true)
                    setPinchZoom(true)
                    setDrawGridBackground(false)
                    setDrawBarShadow(false)
                    setDrawValueAboveBar(true)

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        granularity = 1f
                    }

                    axisRight.isEnabled = false
                    axisLeft.axisMinimum = 0f

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
                chart.xAxis.textColor = textColor
                chart.axisLeft.textColor = textColor
                chart.axisLeft.gridColor = gridColor
                chart.legend.textColor = textColor

                chart.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return xLabels[value] ?: value.toString()
                    }
                }
                
                chart.data = barData
                chart.invalidate()
            }
        )
    }
}

@Composable
fun BloodPressurePieChart(
    pieData: PieData?,
    modifier: Modifier = Modifier,
    onChartReady: ((() -> Bitmap) -> Unit)? = null
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PieChart(context).apply {
                    description.isEnabled = false
                    setUsePercentValues(true)
                    setExtraOffsets(5f, 10f, 5f, 5f)
                    dragDecelerationFrictionCoef = 0.95f
                    isDrawHoleEnabled = true
                    setTransparentCircleColor(AndroidColor.WHITE)
                    setTransparentCircleAlpha(110)
                    holeRadius = 58f
                    transparentCircleRadius = 61f
                    setDrawCenterText(true)
                    rotationAngle = 0f
                    isRotationEnabled = true
                    isHighlightPerTapEnabled = true

                    legend.apply {
                        isEnabled = true
                        orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
                        horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
                        verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                        setDrawInside(false)
                        xEntrySpace = 7f
                        yEntrySpace = 0f
                        yOffset = 0f
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
                chart.setHoleColor(backgroundColor)
                chart.setEntryLabelColor(textColor)
                chart.legend.textColor = textColor
                
                chart.data = pieData
                chart.invalidate()
            }
        )
    }
}
