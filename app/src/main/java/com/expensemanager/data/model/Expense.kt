package com.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val date: Long,
    val isRecurring: Boolean = false,
    val recurringIntervalDays: Int? = null,
)
