package com.expensemanager.utils

object Constants {
    const val TYPE_INCOME = "Income"
    const val TYPE_EXPENSE = "Expense"

    val EXPENSE_CATEGORIES = listOf(
        "Food",
        "Travel",
        "Shopping",
        "Bills",
        "Health",
        "Entertainment",
        "Education",
        "Other",
    )

    val INCOME_CATEGORIES = listOf(
        "Salary",
        "Freelance",
        "Investment",
        "Gift",
        "Other",
    )

    fun categoriesForType(type: String): List<String> =
        if (type == TYPE_INCOME) INCOME_CATEGORIES else EXPENSE_CATEGORIES

    const val CHANNEL_RECURRING_ID = "recurring_expenses"
}
