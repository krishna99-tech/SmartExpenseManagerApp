package com.expensemanager.utils

import android.content.Context
import com.expensemanager.data.model.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun buildCsv(expenses: List<Expense>): String = buildString {
        appendLine("id,title,amount,category,type,date_iso,recurring,interval_days")
        expenses.forEach { e ->
            append(e.id).append(',')
            append('"').append(e.title.replace("\"", "\"\"")).append("\",")
            append(e.amount).append(',')
            append('"').append(e.category).append("\",")
            append('"').append(e.type).append("\",")
            append(e.date).append(',')
            append(if (e.isRecurring) "1" else "0").append(',')
            appendLine(e.recurringIntervalDays ?: "")
        }
    }

    fun writeExportFile(context: Context, expenses: List<Expense>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "expenses_$stamp.csv")
        file.writeText(buildCsv(expenses))
        return file
    }
}
