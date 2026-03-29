package com.expensemanager.data.repository

import com.expensemanager.data.local.ExpenseDao
import com.expensemanager.data.model.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {
    fun observeAll(): Flow<List<Expense>> = dao.observeAll()

    suspend fun insert(expense: Expense) {
        dao.insert(expense)
    }

    suspend fun delete(expense: Expense) {
        dao.delete(expense)
    }

    suspend fun transactionsInRange(startMillis: Long, endMillis: Long): List<Expense> =
        dao.getBetween(startMillis, endMillis)

    suspend fun recurringExpenses(): List<Expense> = dao.getRecurringExpenses()
}
