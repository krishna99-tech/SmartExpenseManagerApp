package com.expensemanager

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.expensemanager.data.local.AppDatabase
import com.expensemanager.data.preferences.UserPreferences
import com.expensemanager.work.RecurringReminderWorker
import java.util.concurrent.TimeUnit

class ExpenseManagerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME_RECURRING,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        private const val WORK_NAME_RECURRING = "recurring_expense_reminder"
    }
}
