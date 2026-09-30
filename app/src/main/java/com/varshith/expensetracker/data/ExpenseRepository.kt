package com.varshith.expensetracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun createExpense(
        title: String,
        amount: Double,
        category: String,
        dateMillis: Long,
        recurringExpenseId: Long? = null
    ) {
        dao.insert(
            Expense(
                title = title,
                amount = amount,
                category = category,
                dateMillis = dateMillis,
                recurringExpenseId = recurringExpenseId
            )
        )
    }

    suspend fun getExpenseById(id: Long): Expense? = dao.getExpenseById(id)

    suspend fun deleteExpense(expense: Expense) = dao.delete(expense)

    suspend fun deleteExpenseById(id: Long) = dao.deleteById(id)

    suspend fun updateExpense(expense: Expense) = dao.update(expense)

    suspend fun restoreExpense(expense: Expense) = dao.insert(expense)
}
