package com.varshith.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.varshith.expensetracker.data.RecurrenceFrequency
import com.varshith.expensetracker.data.RecurringExpense
import com.varshith.expensetracker.data.RecurringExpenseDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringViewModel(private val dao: RecurringExpenseDao) : ViewModel() {

    val recurringExpenses: StateFlow<List<RecurringExpense>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecurring(title: String, amount: Double, category: String, frequency: RecurrenceFrequency, startMillis: Long) {
        viewModelScope.launch {
            dao.insert(
                RecurringExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    nextOccurrenceMillis = startMillis
                )
            )
        }
    }

    fun toggleActive(recurring: RecurringExpense) {
        viewModelScope.launch { dao.update(recurring.copy(isActive = !recurring.isActive)) }
    }

    fun delete(recurring: RecurringExpense) {
        viewModelScope.launch { dao.delete(recurring) }
    }

    companion object {
        fun factory(dao: RecurringExpenseDao) = viewModelFactory {
            initializer { RecurringViewModel(dao) }
        }
    }
}