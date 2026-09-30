package com.varshith.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: RecurrenceFrequency,
    val nextOccurrenceMillis: Long,
    val isActive: Boolean = true
)