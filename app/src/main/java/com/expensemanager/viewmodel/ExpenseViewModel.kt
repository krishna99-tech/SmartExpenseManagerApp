package com.expensemanager.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensemanager.ExpenseManagerApp
import com.expensemanager.R
import com.expensemanager.data.model.Expense
import com.expensemanager.data.preferences.UserPreferences
import com.expensemanager.data.repository.ExpenseRepository
import com.expensemanager.utils.BillScanParser
import com.expensemanager.utils.BillScanSuggestion
import com.expensemanager.utils.BillTextRecognizer
import com.expensemanager.utils.Constants
import com.expensemanager.utils.ExportUtils
import com.expensemanager.utils.SmartInsights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class BudgetWarningLevel {
    None,
    Near,
    Critical,
    Exceeded,
}

data class DashboardSummary(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val budgetExceeded: Boolean = false,
    val budgetWarningLevel: BudgetWarningLevel = BudgetWarningLevel.None,
    val budgetUsageRatio: Double = 0.0,
)

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val preferences: UserPreferences,
) : AndroidViewModel(application) {

    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _billScanSuggestion = MutableStateFlow<BillScanSuggestion?>(null)
    val billScanSuggestion: StateFlow<BillScanSuggestion?> = _billScanSuggestion.asStateFlow()

    val expenses: StateFlow<List<Expense>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlyBudgetCap: StateFlow<Double?> = preferences.monthlyBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val insightStrings: SmartInsights.InsightStrings by lazy {
        val app = getApplication<Application>()
        SmartInsights.InsightStrings(
            spentMoreCategoryWeek = { c ->
                app.getString(R.string.insight_spent_more_week, c)
            },
            highShareCategory = { c, pct ->
                app.getString(R.string.insight_high_share_category, c, pct)
            },
            reduceTravel = app.getString(R.string.insight_reduce_travel),
            spendingCloseToIncome = app.getString(R.string.insight_spending_near_income),
        )
    }

    val smartInsights: StateFlow<List<String>> = expenses.map { list ->
        SmartInsights.generate(list, insightStrings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<DashboardSummary> = combine(
        expenses,
        preferences.monthlyBudget,
    ) { list, budget ->
        val income = list.filter { it.type == Constants.TYPE_INCOME }.sumOf { it.amount }
        val expense = list.filter { it.type == Constants.TYPE_EXPENSE }.sumOf { it.amount }
        val range = currentMonthRangeMillis()
        val monthlyExpense = list.filter {
            it.type == Constants.TYPE_EXPENSE && it.date >= range.first && it.date < range.second
        }.sumOf { it.amount }
        val budgetExceeded = budget != null && budget > 0 && monthlyExpense > budget
        val ratio = if (budget != null && budget > 0) monthlyExpense / budget else 0.0
        val level = when {
            budget == null || budget <= 0 -> BudgetWarningLevel.None
            monthlyExpense > budget -> BudgetWarningLevel.Exceeded
            ratio >= 0.9 -> BudgetWarningLevel.Critical
            ratio >= 0.8 -> BudgetWarningLevel.Near
            else -> BudgetWarningLevel.None
        }
        DashboardSummary(
            balance = income - expense,
            totalIncome = income,
            totalExpense = expense,
            monthlyExpense = monthlyExpense,
            budgetExceeded = budgetExceeded,
            budgetWarningLevel = level,
            budgetUsageRatio = ratio,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSummary())

    fun unlockSession() {
        _sessionUnlocked.value = true
    }

    fun lockSession() {
        _sessionUnlocked.value = false
    }

    fun verifyPin(pin: String): Boolean = preferences.verifyPin(pin)

    fun scanBill(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val text = BillTextRecognizer.recognizeText(bitmap)
                val parsed = BillScanParser.parse(text)
                withContext(Dispatchers.Main) {
                    _billScanSuggestion.value = parsed
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _message.value = getApplication<Application>().getString(R.string.scan_failed)
                }
            }
        }
    }

    fun consumeBillScanSuggestion() {
        _billScanSuggestion.value = null
    }

    fun saveTransaction(
        title: String,
        amount: Double,
        category: String,
        type: String,
        isRecurring: Boolean,
        recurringDays: Int?,
        dateMillis: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch {
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                category = category,
                type = type,
                date = dateMillis,
                isRecurring = isRecurring,
                recurringIntervalDays = if (isRecurring) recurringDays?.coerceAtLeast(1) ?: 7 else null,
            )
            repository.insert(expense)
            _message.value = getApplication<Application>().getString(R.string.snackbar_saved)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.delete(expense) }
    }

    fun exportCsv(): java.io.File {
        val list = expenses.value
        return ExportUtils.writeExportFile(getApplication(), list)
    }

    fun clearMessage() {
        _message.value = null
    }

    suspend fun transactionsForReportMonth(
        year: Int,
        monthZeroBased: Int,
    ): List<Expense> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return repository.transactionsInRange(start, end)
    }

    private fun currentMonthRangeMillis(): Pair<Long, Long> {
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

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = application as ExpenseManagerApp
            val repo = ExpenseRepository(app.database.expenseDao())
            return ExpenseViewModel(application, repo, app.userPreferences) as T
        }
    }
}
