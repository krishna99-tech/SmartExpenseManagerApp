package com.expensemanager.utils

import com.expensemanager.data.model.Expense
import java.util.Calendar
import java.util.Locale

object SmartInsights {

    fun generate(
        expenses: List<Expense>,
        strings: InsightStrings,
    ): List<String> {
        if (expenses.isEmpty()) return emptyList()
        val out = mutableListOf<String>()

        val now = Calendar.getInstance()
        val thisWeek = weekRangeMillis(now, 0)
        val lastWeek = weekRangeMillis(now, -1)

        fun expensesIn(range: Pair<Long, Long>) = expenses.filter { it.date >= range.first && it.date < range.second }

        val expThis = expensesIn(thisWeek).filter { it.type == Constants.TYPE_EXPENSE }
        val expLast = expensesIn(lastWeek).filter { it.type == Constants.TYPE_EXPENSE }

        val byCatThis = expThis.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val byCatLast = expLast.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }

        byCatThis.forEach { (cat, amt) ->
            val prev = byCatLast[cat] ?: 0.0
            if (prev > 0 && amt > prev * 1.25) {
                out += strings.spentMoreCategoryWeek(cat)
            }
        }

        val monthRange = monthRangeMillis(now)
        val monthExpenses = expenses.filter {
            it.type == Constants.TYPE_EXPENSE && it.date >= monthRange.first && it.date < monthRange.second
        }
        val totalMonth = monthExpenses.sumOf { it.amount }
        if (totalMonth > 0) {
            val byCatMonth = monthExpenses.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
            val top = byCatMonth.maxBy { it.value }
            val share = top.value / totalMonth
            if (share >= 0.35) {
                out += strings.highShareCategory(top.key, (share * 100).toInt())
            }
            val travel = byCatMonth["Travel"] ?: 0.0
            if (travel / totalMonth >= 0.25) {
                out += strings.reduceTravel
            }
        }

        val incomeMonth = expenses.filter {
            it.type == Constants.TYPE_INCOME && it.date >= monthRange.first && it.date < monthRange.second
        }.sumOf { it.amount }
        val expenseMonth = monthExpenses.sumOf { it.amount }
        if (incomeMonth > 0 && expenseMonth > incomeMonth * 0.95) {
            out += strings.spendingCloseToIncome
        }

        return out.distinct().take(6)
    }

    private fun weekRangeMillis(now: Calendar, weekOffset: Int): Pair<Long, Long> {
        val cal = now.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 7)
        val end = cal.timeInMillis
        return start to end
    }

    private fun monthRangeMillis(now: Calendar): Pair<Long, Long> {
        val cal = now.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    data class InsightStrings(
        val spentMoreCategoryWeek: (String) -> String,
        val highShareCategory: (String, Int) -> String,
        val reduceTravel: String,
        val spendingCloseToIncome: String,
    )
}
