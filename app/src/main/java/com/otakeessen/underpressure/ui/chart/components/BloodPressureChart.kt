package com.otakeessen.underpressure.ui.chart.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.ValueFormatter
import com.otakeessen.underpressure.ui.chart.ChartUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BloodPressureChart(
    uiState: ChartUiState,
    lineData: LineData?,
    startDate: LocalDate?,
    xLabels: Map<Float, String> = emptyMap(),
    modifier: Modifier = Modifier,
    showXAxisLabels: Boolean = true,
    showRiskZones: Boolean = false,
    onChartReady: ((() -> Bitmap) -> Unit)? = null
) {

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }
    var chartRef by remember { mutableStateOf<LineChart?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            RiskZoneLineChart(context).apply {
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
                    labelCount = 5
                    setAvoidFirstLastClipping(true)
                }

                axisRight.isEnabled = false

                extraBottomOffset = 16f

                legend.apply {
                    isEnabled = true
                    isWordWrapEnabled = true
                }

                chartRef = this

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
            val riskChart = chart as? RiskZoneLineChart
            riskChart?.showRiskZones = showRiskZones
            
            chart.marker = BloodPressureMarkerView(chart, startDate, xLabels, uiState.trackerValuesMap, uiState.trackerDefinitionsMap)

            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.legend.textColor = textColor

            chart.axisLeft.gridColor = gridColor
            chart.xAxis.gridColor = gridColor

            chart.xAxis.setDrawLabels(showXAxisLabels)

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return xLabels[value] ?: startDate?.plusDays(value.toLong())?.format(dateFormatter) ?: value.toString()
                }
            }

            chart.axisLeft.removeAllLimitLines()

            if (!showRiskZones) {
                chart.axisLeft.resetAxisMinimum()

                val limit140 = LimitLine(140f).apply {
                    lineColor = AndroidColor.parseColor("#C0392B")
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                }
                val limit90 = LimitLine(90f).apply {
                    lineColor = AndroidColor.parseColor("#C0392B")
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                }
                val limit130 = LimitLine(130f).apply {
                    lineColor = AndroidColor.parseColor("#E67E22")
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                }
                val limit80 = LimitLine(80f).apply {
                    lineColor = AndroidColor.parseColor("#E67E22")
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                }

                chart.axisLeft.addLimitLine(limit140)
                chart.axisLeft.addLimitLine(limit90)
                chart.axisLeft.addLimitLine(limit130)
                chart.axisLeft.addLimitLine(limit80)
            }

            if (chart.data !== lineData) {
                chart.highlightValues(null)
                chart.data = lineData
            }
            chart.invalidate()
        }
    )
}

private class RiskZoneLineChart(context: android.content.Context) : LineChart(context) {
    var showRiskZones: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val bands = listOf(
        180f to AndroidColor.rgb(255, 189, 187),
        140f to AndroidColor.rgb(255, 205, 210),
        130f to AndroidColor.rgb(255, 224, 178),
        120f to AndroidColor.rgb(255, 249, 196),
        80f to AndroidColor.rgb(232, 245, 233),
    )

    private val paint = android.graphics.Paint()

    override fun onDraw(canvas: android.graphics.Canvas) {
        if (showRiskZones) {
            drawRiskZones(canvas)
        }
        super.onDraw(canvas)
    }

    private fun drawRiskZones(canvas: android.graphics.Canvas) {
        val vph = viewPortHandler
        val transformer = getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)

        val cLeft = vph.contentLeft()
        val cTop = vph.contentTop()
        val cRight = vph.contentRight()
        val cBottom = vph.contentBottom()

        if (cRight - cLeft <= 0f || cBottom - cTop <= 0f) return

        for ((threshold, colorInt) in bands) {
            val pt = transformer.getPixelForValues(0f, threshold)
            val ptY = pt.y.toFloat()

            val topY = ptY.coerceIn(cTop, cBottom)
            val bandBottomY = cBottom

            paint.color = colorInt
            canvas.drawRect(cLeft, topY, cRight, bandBottomY, paint)
        }
    }
}
