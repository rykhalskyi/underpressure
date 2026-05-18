package com.otakeessen.underpressure.ui.chart.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.otakeessen.underpressure.R

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
    val context = LocalContext.current
    val localizedLabels = remember(xLabels, context) {
        buildLocalizedLabelMap(context, xLabels)
    }
    
    val barValueFormatter = remember(localizedLabels, xLabels) {
        object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return localizedLabels[value] ?: xLabels[value] ?: value.toString()
            }
        }
    }

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
                chart.xAxis.valueFormatter = barValueFormatter
                
                chart.data = barData
                chart.data?.dataSets?.forEach { it.label = context.getString(R.string.label_bar_frequency) }
                chart.invalidate()
            }
        )
    }
}

private class SafePieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : PieChartRenderer(chart, animator, viewPortHandler) {
    override fun drawExtras(c: Canvas) {
        drawHole(c)
        // Accessing protected mDrawBitmap from Java class. 
        // mDrawBitmap can be null if drawData hasn't been called or if width/height are 0.
        mDrawBitmap?.get()?.let { bitmap ->
            c.drawBitmap(bitmap, 0f, 0f, null)
        }
        drawCenterText(c)
    }
}

private class SafePieChart(context: Context) : PieChart(context) {
    init {
        // Use custom renderer that handles null bitmap cache to prevent NPE during transitions
        renderer = SafePieChartRenderer(this, animator, viewPortHandler)
    }

    override fun onDetachedFromWindow() {
        data = null
        super.onDetachedFromWindow()
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
    val context = LocalContext.current

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                SafePieChart(context).apply {
                    description.isEnabled = false
                    setUsePercentValues(true)
                    setExtraOffsets(5f, 10f, 5f, 5f)
                    dragDecelerationFrictionCoef = 0.95f
                    isDrawHoleEnabled = true
                    setTransparentCircleColor(AndroidColor.WHITE)
                    setTransparentCircleAlpha(110)
                    holeRadius = 58f
                    transparentCircleRadius = 61f
                    setDrawCenterText(false)
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
                chart.setDrawEntryLabels(false) // Hide labels on the chart slices

                pieData?.dataSets?.forEach { ds ->
                    val dataSet = ds as? PieDataSet
                    dataSet?.values?.forEach { entry ->
                        // Labels remain on entries so they appear in the legend
                        entry.label = resolveLevelLabel(context, entry.label)
                    }
                    ds.label = context.getString(R.string.label_pie_distribution)
                }

                chart.data = pieData
                chart.invalidate()
            }
        )
    }
}

private fun resolveLevelLabel(context: Context, label: String?): String {
    if (label == null) return ""
    val resId = when (label.replace(" ", "_")) {
        "HYPOTENSION" -> R.string.bp_level_hypotension
        "NORMAL" -> R.string.bp_level_normal
        "ELEVATED" -> R.string.bp_level_elevated
        "STAGE_1" -> R.string.bp_level_stage1
        "STAGE_2" -> R.string.bp_level_stage2
        "CRISIS" -> R.string.bp_level_crisis
        else -> return label
    }
    return context.getString(resId)
}

private fun buildLocalizedLabelMap(context: Context, xLabels: Map<Float, String>): Map<Float, String> {
    return xLabels.mapValues { (_, value) ->
        resolveLevelLabel(context, value)
    }
}
