package com.example.underpressure.ui.chart.components

import android.content.Context
import android.widget.TextView
import com.example.underpressure.R
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BloodPressureMarkerView(
    private val chart: Chart<*>,
    private val startDate: LocalDate?
) : MarkerView(chart.context, R.layout.chart_marker_view) {

    private val tvDate: TextView = findViewById(R.id.tv_date)
    private val tvValue: TextView = findViewById(R.id.tv_value)
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val dateText = startDate?.plusDays(e.x.toLong())?.format(dateFormatter) ?: e.x.toString()
        
        val sb = StringBuilder()
        val data = chart.data
        if (data != null) {
            for (i in 0 until data.dataSetCount) {
                val dataSet = data.getDataSetByIndex(i)
                val entryAtX = dataSet.getEntryForXValue(e.x, Float.NaN)
                if (entryAtX != null && entryAtX.x == e.x) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append("${dataSet.label}: ${entryAtX.y.toInt()}")
                }
            }
        }
        
        tvDate.text = dateText
        tvValue.text = sb.toString()

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
