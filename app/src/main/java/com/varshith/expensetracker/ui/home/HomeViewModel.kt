package com.varshith.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.varshith.expensetracker.data.Expense
import com.varshith.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryTotal(
    val category: String,
    val total: Double,
    val fraction: Float
)

class HomeViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotal: StateFlow<Double> = expenses
        .map { list -> list.filter { isThisMonth(it.dateMillis) }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyTransactionCount: StateFlow<Int> = expenses
        .map { list -> list.count { isThisMonth(it.dateMillis) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryBreakdown: StateFlow<List<CategoryTotal>> = expenses
        .map { list ->
            val thisMonth = list.filter { isThisMonth(it.dateMillis) }
            val total = thisMonth.sumOf { it.amount }
            if (total <= 0.0) return@map emptyList()
            thisMonth.groupBy { it.category }
                .map { (category, items) ->
                    val sum = items.sumOf { it.amount }
                    CategoryTotal(category, sum, (sum / total).toFloat())
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isThisMonth(millis: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch { repository.restoreExpense(expense) }
    }

    companion object {
        fun factory(repository: ExpenseRepository) = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}