package com.expensemanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun CategoryPieChart(
    categoryTotals: Map<String, Double>,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                legend.isWordWrapEnabled = true
                legend.textSize = 12f
                setUsePercentValues(false)
                isDrawHoleEnabled = true
                holeRadius = 42f
                transparentCircleRadius = 47f
                setHoleColor(android.graphics.Color.TRANSPARENT)
                setDrawCenterText(false)
                rotationAngle = 0f
                isRotationEnabled = true
                isHighlightPerTapEnabled = true
            }
        },
        update = { chart ->
            if (categoryTotals.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }
            val entries = categoryTotals.entries
                .filter { it.value > 0 }
                .map { PieEntry(it.value.toFloat(), it.key) }
            if (entries.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }
            val dataSet = PieDataSet(entries, "").apply {
                sliceSpace = 2f
                selectionShift = 5f
                valueTextSize = 11f
                colors = buildList {
                    ColorTemplate.MATERIAL_COLORS.forEach { add(it) }
                    ColorTemplate.COLORFUL_COLORS.forEach { add(it) }
                    ColorTemplate.PASTEL_COLORS.forEach { add(it) }
                }
            }
            val data = PieData(dataSet)
            data.setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    if (value >= 1000f) String.format("%.0f", value) else String.format("%.1f", value)
            })
            chart.data = data
            chart.invalidate()
        },
    )
}
