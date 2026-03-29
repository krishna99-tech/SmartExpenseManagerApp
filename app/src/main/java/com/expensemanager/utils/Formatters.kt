package com.expensemanager.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val dateShortFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

    fun currency(value: Double): String = currencyFormat.format(value)

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun dateShort(millis: Long): String = dateShortFormat.format(Date(millis))
}
