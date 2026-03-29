package com.expensemanager.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class BillScanSuggestion(
    val amount: Double?,
    val dateMillis: Long?,
    val suggestedTitle: String?,
)

object BillScanParser {

    private val moneyPattern = Pattern.compile(
        "(?:total|amount|due|balance|paid)\\s*[:\\s]*" +
            "[$€£₹]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)|" +
            "[$€£₹]\\s*([0-9]+(?:[.,][0-9]{1,2})?)|" +
            "([0-9]+(?:[.,][0-9]{1,2})?)\\s*[$€£₹]",
        Pattern.CASE_INSENSITIVE,
    )

    private val looseMoney = Pattern.compile("\\b([0-9]+[.,][0-9]{2})\\b")

    private val datePatterns = listOf(
        SimpleDateFormat("dd/MM/yy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
    )

    fun parse(ocrText: String): BillScanSuggestion {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val title = lines.firstOrNull { it.length in 3..48 && !it.any { ch -> ch.isDigit() && it.count { d -> d.isDigit() } > 6 } }
            ?: lines.firstOrNull()

        var amount: Double? = null
        val matcher = moneyPattern.matcher(ocrText)
        val candidates = mutableListOf<Double>()
        while (matcher.find()) {
            val g1 = matcher.group(1) ?: matcher.group(2) ?: matcher.group(3) ?: continue
            parseAmount(g1)?.let { candidates.add(it) }
        }
        if (candidates.isEmpty()) {
            val m2 = looseMoney.matcher(ocrText)
            while (m2.find()) {
                parseAmount(m2.group(1)!!)?.let { candidates.add(it) }
            }
        }
        if (candidates.isNotEmpty()) {
            amount = candidates.maxOrNull()
        }

        var dateMs: Long? = null
        for (line in lines) {
            dateMs = parseDateLine(line)
            if (dateMs != null) break
        }
        if (dateMs == null) {
            dateMs = parseDateLine(ocrText.replace("\n", " "))
        }

        return BillScanSuggestion(
            amount = amount,
            dateMillis = dateMs,
            suggestedTitle = title?.take(60),
        )
    }

    private fun parseAmount(raw: String): Double? {
        val normalized = raw.replace(",", ".")
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value in 0.01..999_999.0) value else null
    }

    private fun parseDateLine(line: String): Long? {
        for (fmt in datePatterns) {
            try {
                fmt.isLenient = false
                val d = fmt.parse(line) ?: continue
                val cal = Calendar.getInstance()
                cal.time = d
                if (cal.get(Calendar.YEAR) in 2000..2100) return cal.timeInMillis
            } catch (_: ParseException) {
                continue
            }
        }
        return Regex("(\\d{1,2})/(\\d{1,2})/(\\d{2,4})").find(line)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return@let null
            val month = m.groupValues[2].toIntOrNull() ?: return@let null
            var year = m.groupValues[3].toIntOrNull() ?: return@let null
            if (year < 100) year += 2000
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day, 12, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
}
