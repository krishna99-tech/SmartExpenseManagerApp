package com.expensemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensemanager.R
import com.expensemanager.data.model.Expense
import com.expensemanager.ui.theme.DsSpacing
import com.expensemanager.utils.Constants
import com.expensemanager.utils.Formatters

@Composable
fun ExpenseItem(
    expense: Expense,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncome = expense.type == Constants.TYPE_INCOME
    val amountColor = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = DsSpacing.md, vertical = DsSpacing.sm)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isIncome) Icons.Outlined.NorthEast else Icons.Outlined.SouthWest,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.padding(end = DsSpacing.xs),
                    )
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = Formatters.dateTime(expense.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = DsSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tag(text = expense.category)
                    if (expense.isRecurring) {
                        Tag(text = stringResource(R.string.label_recurring_short))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+" else "-") + Formatters.currency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_delete_transaction),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = DsSpacing.sm, vertical = DsSpacing.xxs),
        )
    }
}
