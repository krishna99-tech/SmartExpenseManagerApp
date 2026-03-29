package com.expensemanager.ui.screen

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensemanager.R
import com.expensemanager.utils.Constants
import com.expensemanager.viewmodel.ExpenseViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(Constants.TYPE_EXPENSE) }
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var recurring by rememberSaveable { mutableStateOf(false) }
    var recurringDaysText by rememberSaveable { mutableStateOf("7") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var entryDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var decoding by remember { mutableStateOf(false) }

    val scanSuggestion by viewModel.billScanSuggestion.collectAsStateWithLifecycle()
    LaunchedEffect(scanSuggestion) {
        val s = scanSuggestion ?: return@LaunchedEffect
        s.amount?.let { amountText = String.format(Locale.getDefault(), "%.2f", it) }
        s.suggestedTitle?.let { if (title.isBlank()) title = it }
        s.dateMillis?.let { entryDateMillis = it }

        // Try to improve autofill: guess income/expense + category from the recognized title.
        val recognized = (s.suggestedTitle ?: "").lowercase(Locale.getDefault())
        val guessedType =
            if (recognized.contains("salary") || recognized.contains("income") || recognized.contains("pay") || recognized.contains("credited")) {
                Constants.TYPE_INCOME
            } else {
                Constants.TYPE_EXPENSE
            }
        val guessedCategory = when {
            listOf("restaurant", "grocery", "supermarket", "market", "food", "cafe").any { recognized.contains(it) } -> "Food"
            listOf("uber", "taxi", "flight", "hotel", "travel", "train", "bus").any { recognized.contains(it) } -> "Travel"
            listOf("electric", "internet", "water", "gas", "bill", "utility").any { recognized.contains(it) } -> "Bills"
            else -> null
        }

        type = guessedType
        val cats = Constants.categoriesForType(guessedType)
        categoryIndex = if (guessedCategory != null) {
            cats.indexOfFirst { it.equals(guessedCategory, ignoreCase = true) }.coerceAtLeast(0)
        } else {
            0
        }

        viewModel.consumeBillScanSuggestion()
    }

    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            decoding = true
            try {
                val bitmap = withContext(Dispatchers.IO) { decodeBitmap(context, uri) }
                viewModel.scanBill(bitmap)
            } finally {
                decoding = false
            }
        }
    }

    val categories = Constants.categoriesForType(type)
    val safeCategoryIndex = categoryIndex.coerceIn(0, (categories.size - 1).coerceAtLeast(0))
    val category = categories[safeCategoryIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.add_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )

        OutlinedButton(
            onClick = {
                pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            enabled = !decoding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (decoding) stringResource(R.string.scan_processing)
                else stringResource(R.string.scan_bill),
            )
        }
        Text(
            text = stringResource(R.string.scan_bill_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (decoding) {
            BillScanLoading()
        }

        RowOfTypeChips(
            selected = type,
            onSelect = {
                type = it
                categoryIndex = 0
            },
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; error = null },
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                if (raw.all { ch -> ch.isDigit() || ch == '.' || ch == ',' }) {
                    amountText = raw.replace(',', '.')
                    error = null
                }
            },
            label = { Text(stringResource(R.string.field_amount)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Text(text = stringResource(R.string.field_category), style = MaterialTheme.typography.titleSmall)
        CategoryChipsGrid(
            categories = categories,
            selectedIndex = safeCategoryIndex,
            onSelect = { categoryIndex = it },
        )

        RowOfRecurring(
            recurring = recurring,
            onRecurringChange = { recurring = it },
        )

        if (recurring) {
            OutlinedTextField(
                value = recurringDaysText,
                onValueChange = { raw ->
                    if (raw.all { d -> d.isDigit() }) recurringDaysText = raw
                },
                label = { Text(stringResource(R.string.field_recurring_days)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                val parsed = amountText.toDoubleOrNull()
                when {
                    title.isBlank() -> error = context.getString(R.string.error_title)
                    parsed == null || parsed <= 0 -> error = context.getString(R.string.error_amount)
                    recurring && (recurringDaysText.toIntOrNull() ?: 0) <= 0 ->
                        error = context.getString(R.string.error_recurring_days)
                    else -> {
                        viewModel.saveTransaction(
                            title = title,
                            amount = parsed,
                            category = category,
                            type = type,
                            isRecurring = recurring,
                            recurringDays = if (recurring) recurringDaysText.toIntOrNull() else null,
                            dateMillis = entryDateMillis,
                        )
                        title = ""
                        amountText = ""
                        recurring = false
                        recurringDaysText = "7"
                        entryDateMillis = System.currentTimeMillis()
                        error = null
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.action_save_transaction))
        }
    }
}

private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap {
    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    val maxSide = 1400
    val w = bmp.width
    val h = bmp.height
    if (w <= maxSide && h <= maxSide) return bmp
    val scale = maxSide.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(
        bmp,
        (w * scale).toInt().coerceAtLeast(1),
        (h * scale).toInt().coerceAtLeast(1),
        true,
    )
}

@Composable
private fun RowOfTypeChips(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.field_type), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selected == Constants.TYPE_EXPENSE,
                onClick = { onSelect(Constants.TYPE_EXPENSE) },
                label = { Text(stringResource(R.string.type_expense)) },
            )
            FilterChip(
                selected = selected == Constants.TYPE_INCOME,
                onClick = { onSelect(Constants.TYPE_INCOME) },
                label = { Text(stringResource(R.string.type_income)) },
            )
        }
    }
}

@Composable
private fun CategoryChipsGrid(
    categories: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                rowItems.forEach { label ->
                    val index = categories.indexOf(label)
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowOfRecurring(recurring: Boolean, onRecurringChange: (Boolean) -> Unit) {
    Column {
        Text(stringResource(R.string.field_recurring), style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            FilterChip(
                selected = !recurring,
                onClick = { onRecurringChange(false) },
                label = { Text(stringResource(R.string.recurring_off)) },
            )
            FilterChip(
                selected = recurring,
                onClick = { onRecurringChange(true) },
                label = { Text(stringResource(R.string.recurring_on)) },
            )
        }
    }
}

@Composable
private fun BillScanLoading() {
    val transition = rememberInfiniteTransition(label = "billScanLoading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer { rotationZ = rotation }
                .padding(end = 10.dp),
        )
        Text(
            text = stringResource(R.string.scan_processing),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
