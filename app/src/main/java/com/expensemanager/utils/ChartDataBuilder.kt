package com.expensemanager.utils

import com.expensemanager.data.model.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class LabeledFloat(val label: String, val value: Float)

object ChartDataBuilder {

    fun weeklyExpenseTrend(expenses: List<Expense>, weeksBack: Int = 8): List<LabeledFloat> {
        val now = Calendar.getInstance()
        val list = mutableListOf<LabeledFloat>()
        val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
        for (offset in (weeksBack - 1) downTo 0) {
            val range = weekRangeEnding(now, offset)
            val sum = expenses
                .filter { it.type == Constants.TYPE_EXPENSE && it.date >= range.first && it.date < range.second }
                .sumOf { it.amount }
                .toFloat()
            val labelCal = Calendar.getInstance().apply { timeInMillis = range.first }
            list += LabeledFloat(fmt.format(labelCal.time), sum)
        }
        return list
    }

    fun monthlyExpenseTotals(expenses: List<Expense>, monthsBack: Int = 6): List<LabeledFloat> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -(monthsBack - 1))
        val fmt = SimpleDateFormat("MMM yy", Locale.getDefault())
        val list = mutableListOf<LabeledFloat>()
        repeat(monthsBack) {
            val start = cal.timeInMillis
            val label = fmt.format(cal.time)
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            val sum = expenses
                .filter { it.type == Constants.TYPE_EXPENSE && it.date >= start && it.date < end }
                .sumOf { it.amount }
                .toFloat()
            list += LabeledFloat(label, sum)
        }
        return list
    }

    fun cumulativeSavingsSeries(expenses: List<Expense>, weeksBack: Int = 12): List<LabeledFloat> {
        val sorted = expenses.sortedBy { it.date }
        if (sorted.isEmpty()) return emptyList()
        val now = Calendar.getInstance()
        val points = mutableListOf<LabeledFloat>()
        val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
        for (offset in (weeksBack - 1) downTo 0) {
            val range = weekRangeEnding(now, offset)
            val upTo = range.second
            var net = 0.0
            sorted.filter { it.date < upTo }.forEach { e ->
                net += if (e.type == Constants.TYPE_INCOME) e.amount else -e.amount
            }
            val labelCal = Calendar.getInstance().apply { timeInMillis = range.first }
            points += LabeledFloat(fmt.format(labelCal.time), net.toFloat())
        }
        return points
    }

    private fun weekRangeEnding(now: Calendar, weeksAgo: Int): Pair<Long, Long> {
        val cal = now.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 7)
        val end = cal.timeInMillis
        return start to end
    }
}
