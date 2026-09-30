package com.varshith.expensetracker.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.varshith.expensetracker.ExpenseTrackerApp
import com.varshith.expensetracker.data.RecurrenceFrequency
import java.util.Calendar

class RecurringExpenseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ExpenseTrackerApp
        val recurringDao = app.database.recurringExpenseDao()
        val expenseRepository = app.repository

        val now = System.currentTimeMillis()
        val due = recurringDao.getDueRecurringExpenses(now)

        due.forEach { recurring ->
            expenseRepository.createExpense(
                title = recurring.title,
                amount = recurring.amount,
                category = recurring.category,
                dateMillis = recurring.nextOccurrenceMillis,
                recurringExpenseId = recurring.id
            )
            val nextOccurrence = advance(recurring.nextOccurrenceMillis, recurring.frequency)
            recurringDao.update(recurring.copy(nextOccurrenceMillis = nextOccurrence))
        }

        return Result.success()
    }

    private fun advance(fromMillis: Long, frequency: RecurrenceFrequency): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = fromMillis }
        when (frequency) {
            RecurrenceFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.timeInMillis
    }

    companion object {
        const val WORK_NAME = "recurring_expense_check"
    }
}