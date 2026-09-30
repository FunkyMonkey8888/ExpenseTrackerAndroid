package com.varshith.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Insert
    suspend fun insert(recurring: RecurringExpense): Long

    @Update
    suspend fun update(recurring: RecurringExpense)

    @Delete
    suspend fun delete(recurring: RecurringExpense)

    @Query("SELECT * FROM recurring_expenses ORDER BY nextOccurrenceMillis ASC")
    fun getAll(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND nextOccurrenceMillis <= :now")
    suspend fun getDueRecurringExpenses(now: Long): List<RecurringExpense>
}