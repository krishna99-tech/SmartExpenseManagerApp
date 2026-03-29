package com.expensemanager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensemanager.R
import com.expensemanager.data.model.Expense
import com.expensemanager.ui.components.CategoryBarChart
import com.expensemanager.ui.components.CategoryPieChart
import com.expensemanager.ui.components.TrendLineChart
import com.expensemanager.utils.ChartDataBuilder
import com.expensemanager.utils.Constants
import com.expensemanager.viewmodel.ExpenseViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var year by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var monthTransactions by remember { mutableStateOf<List<Expense>>(emptyList()) }
    val allExpenses by viewModel.expenses.collectAsStateWithLifecycle()
    var trendWeekly by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(year, month) {
        monthTransactions = viewModel.transactionsForReportMonth(year, month)
    }

    val weeklySeries = remember(allExpenses) { ChartDataBuilder.weeklyExpenseTrend(allExpenses) }
    val monthlySeries = remember(allExpenses) { ChartDataBuilder.monthlyExpenseTotals(allExpenses) }
    val savingsSeries = remember(allExpenses) { ChartDataBuilder.cumulativeSavingsSeries(allExpenses) }

    val expenseByCategory = monthTransactions
        .filter { it.type == Constants.TYPE_EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }

    val monthlyExpenseTotal = expenseByCategory.values.sum()
    val monthlyIncome = monthTransactions
        .filter { it.type == Constants.TYPE_INCOME }
        .sumOf { it.amount }

    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryArgb = MaterialTheme.colorScheme.tertiary.toArgb()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.reports_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp),
        )

        MonthSelector(
            year = year,
            month = month,
            onPrev = {
                if (month == 0) {
                    month = Calendar.DECEMBER
                    year -= 1
                } else {
                    month -= 1
                }
            },
            onNext = {
                if (month == Calendar.DECEMBER) {
                    month = Calendar.JANUARY
                    year += 1
                } else {
                    month += 1
                }
            },
        )

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.report_monthly_totals), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.report_income_line, monthlyIncome),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.report_expense_line, monthlyExpenseTotal),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.report_net_line, monthlyIncome - monthlyExpenseTotal),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Text(text = stringResource(R.string.report_pie_heading), style = MaterialTheme.typography.titleMedium)

        if (expenseByCategory.isEmpty()) {
            Text(
                text = stringResource(R.string.report_no_expense_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CategoryPieChart(categoryTotals = expenseByCategory)

            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    expenseByCategory.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                        RowBetween(cat, amt)
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.charts_advanced_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(stringResource(R.string.chart_weekly_expenses), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = trendWeekly,
                onClick = { trendWeekly = true },
                label = { Text(stringResource(R.string.charts_tab_weekly)) },
            )
            FilterChip(
                selected = !trendWeekly,
                onClick = { trendWeekly = false },
                label = { Text(stringResource(R.string.charts_tab_monthly)) },
            )
        }
        val trendPoints = if (trendWeekly) weeklySeries else monthlySeries
        val trendLabel = if (trendWeekly) {
            stringResource(R.string.chart_weekly_expenses)
        } else {
            stringResource(R.string.chart_monthly_expenses)
        }
        TrendLineChart(
            points = trendPoints,
            label = trendLabel,
            lineColorArgb = primaryArgb,
        )

        Text(
            text = stringResource(R.string.chart_category_compare),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (expenseByCategory.isEmpty()) {
            Text(
                text = stringResource(R.string.report_no_expense_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CategoryBarChart(
                categoryTotals = expenseByCategory,
                barColorArgb = primaryArgb,
            )
        }

        Text(
            text = stringResource(R.string.chart_savings_buildup),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        TrendLineChart(
            points = savingsSeries,
            label = stringResource(R.string.chart_savings_buildup),
            lineColorArgb = tertiaryArgb,
            allowNegativeY = true,
        )
    }
}

@Composable
private fun MonthSelector(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val label = Calendar.getInstance().apply { set(year, month, 1) }
        .getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale.getDefault())
        ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FilterChip(selected = false, onClick = onPrev, label = { Text("←") })
        Text(
            text = "$label $year",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        FilterChip(selected = false, onClick = onNext, label = { Text("→") })
    }
}

@Composable
private fun RowBetween(category: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = category, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = String.format(Locale.getDefault(), "%.2f", amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
