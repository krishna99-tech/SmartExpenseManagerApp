package com.expensemanager.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensemanager.R
import com.expensemanager.data.local.AppDatabase
import com.expensemanager.utils.Constants

class RecurringReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).expenseDao()
        val recurring = dao.getRecurringExpenses()
        if (recurring.isEmpty()) return Result.success()

        ensureChannel(applicationContext)

        val lines = recurring.take(5).joinToString("\n") { item ->
            "${item.title} · ${item.category} (${item.recurringIntervalDays ?: "?"}d)"
        }
        val summary = applicationContext.getString(
            R.string.notification_recurring_summary,
            recurring.size,
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            Constants.CHANNEL_RECURRING_ID,
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.notification_recurring_title))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\n\n$lines"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            Constants.CHANNEL_RECURRING_ID,
            context.getString(R.string.channel_recurring_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 0x4550
    }
}
