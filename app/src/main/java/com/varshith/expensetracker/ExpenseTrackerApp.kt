package com.varshith.expensetracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.varshith.expensetracker.data.AppDatabase
import com.varshith.expensetracker.data.ExpenseRepository
import com.varshith.expensetracker.recurring.RecurringExpenseWorker
import java.util.concurrent.TimeUnit

class ExpenseTrackerApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { ExpenseRepository(database.expenseDao()) }

    override fun onCreate() {
        super.onCreate()
        scheduleRecurringExpenseCheck()
    }

    private fun scheduleRecurringExpenseCheck() {
        val request = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecurringExpenseWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}