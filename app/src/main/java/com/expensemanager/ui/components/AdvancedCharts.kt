package com.expensemanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import com.expensemanager.utils.LabeledFloat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun TrendLineChart(
    points: List<LabeledFloat>,
    modifier: Modifier = Modifier,
    label: String,
    lineColorArgb: Int,
    allowNegativeY: Boolean = false,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setScaleEnabled(false)
                legend.isEnabled = false
                axisRight.isEnabled = false
                setNoDataText("No data yet")
                setNoDataTextColor(android.graphics.Color.GRAY)
                setExtraOffsets(4f, 8f, 12f, 4f)
                if (!allowNegativeY) {
                    axisLeft.axisMinimum = 0f
                }
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setAvoidFirstLastClipping(true)
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(true)
            }
        },
        update = { chart ->
            if (points.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }
            val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.value) }
            val set = LineDataSet(entries, label).apply {
                color = lineColorArgb
                setDrawCircles(true)
                circleRadius = 4f
                lineWidth = 2.2f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setCircleColor(lineColorArgb)
                setDrawFilled(true)
                fillColor = materialColorWithAlpha(lineColorArgb, 0.2f)
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.label })
            chart.xAxis.labelCount = minOf(points.size, 6)
            chart.data = LineData(set)
            if (allowNegativeY) {
                chart.axisLeft.resetAxisMinimum()
            } else {
                chart.axisLeft.axisMinimum = 0f
            }
            chart.invalidate()
        },
    )
}

@Composable
fun CategoryBarChart(
    categoryTotals: Map<String, Double>,
    modifier: Modifier = Modifier,
    barColorArgb: Int,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setScaleEnabled(false)
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                setNoDataText("No data yet")
                setNoDataTextColor(android.graphics.Color.GRAY)
                setExtraOffsets(4f, 8f, 12f, 4f)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setLabelRotationAngle(-18f)
                xAxis.setDrawGridLines(false)
            }
        },
        update = { chart ->
            if (categoryTotals.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }
            val sorted = categoryTotals.entries.filter { it.value > 0 }.sortedByDescending { it.value }
            val entries = sorted.mapIndexed { i, e -> BarEntry(i.toFloat(), e.value.toFloat()) }
            val labels = sorted.map { it.key }
            val set = BarDataSet(entries, "").apply {
                color = barColorArgb
                valueTextSize = 10f
                highLightAlpha = 0
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.labelCount = minOf(labels.size, 8)
            chart.data = BarData(set).apply { barWidth = 0.55f }
            chart.invalidate()
        },
    )
}

fun materialColorWithAlpha(rgb: Int, alpha: Float): Int {
    val a = (255 * alpha.coerceIn(0f, 1f)).toInt()
    return ColorUtils.setAlphaComponent(rgb, a)
}
