package com.expensemanager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensemanager.R
import com.expensemanager.ui.components.ExpenseItem
import com.expensemanager.utils.Constants
import com.expensemanager.viewmodel.BudgetWarningLevel
import com.expensemanager.viewmodel.ExpenseViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val budgetCap by viewModel.monthlyBudgetCap.collectAsStateWithLifecycle()
    val smartInsights by viewModel.smartInsights.collectAsStateWithLifecycle()

    val range = remember { monthRangeMillis() }
    val topCategoryText = remember(expenses) {
        val grouped = expenses
            .filter { it.type == Constants.TYPE_EXPENSE && it.date >= range.first && it.date < range.second }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
        if (grouped.isEmpty()) null
        else grouped.maxBy { it.value }.let { "${it.key} (${String.format(Locale.getDefault(), "%.2f", it.value)})" }
    }
    val recent = remember(expenses) { expenses.take(12) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.dashboard_headline),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            SummaryRow(
                title = stringResource(R.string.label_balance),
                value = summary.balance,
                emphasize = true,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.label_income),
                    value = summary.totalIncome,
                    positive = true,
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.label_expense),
                    value = summary.totalExpense,
                    positive = false,
                )
            }
        }

        item {
            budgetCap?.let { cap ->
                if (cap > 0) {
                    val progress = (summary.monthlyExpense / cap).coerceIn(0.0, 1.0).toFloat()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.label_monthly_budget),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = stringResource(
                                    R.string.budget_usage,
                                    summary.monthlyExpense,
                                    cap,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
        }

        if (summary.budgetWarningLevel == BudgetWarningLevel.Near) {
            item {
                BudgetHintCard(
                    text = stringResource(R.string.alert_budget_near),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
        }
        if (summary.budgetWarningLevel == BudgetWarningLevel.Critical) {
            item {
                BudgetHintCard(
                    text = stringResource(R.string.alert_budget_critical),
                    container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                )
            }
        }
        if (summary.budgetExceeded) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.alert_budget_exceeded),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (smartInsights.isNotEmpty()) {
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.section_smart_insights),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        smartInsights.forEach { line ->
                            Text(
                                text = "• $line",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.card_spending_insight),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = topCategoryText?.let {
                            stringResource(R.string.insight_top_category, it)
                        } ?: stringResource(R.string.insight_no_expenses_month),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.section_recent),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.empty_recent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(recent, key = { it.id }) { expense ->
                ExpenseItem(
                    expense = expense,
                    onDelete = { viewModel.deleteExpense(expense) },
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(title: String, value: Double, emphasize: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = String.format(Locale.getDefault(), "%.2f", value),
                style = if (emphasize) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: Double, positive: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (positive) 0.55f else 0.4f,
            ),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = String.format(Locale.getDefault(), "%.2f", value),
                style = MaterialTheme.typography.titleLarge,
                color = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun BudgetHintCard(text: String, container: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun monthRangeMillis(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
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
