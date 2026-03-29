package com.expensemanager.utils

object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun isDark(mode: String, systemIsDark: Boolean): Boolean = when (mode) {
        DARK -> true
        LIGHT -> false
        else -> systemIsDark
    }
}
