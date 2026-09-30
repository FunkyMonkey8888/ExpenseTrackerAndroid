package com.varshith.expensetracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.varshith.expensetracker.data.Expense
import com.varshith.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class DateRangeFilter(val startMillis: Long?, val endMillis: Long?)

data class SearchFilterState(
    val query: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val dateRange: DateRangeFilter = DateRangeFilter(null, null),
    val minAmount: String = "",
    val maxAmount: String = ""
) {
    val isFilterActive: Boolean
        get() = selectedCategories.isNotEmpty() || dateRange.startMillis != null || dateRange.endMillis != null ||
                minAmount.isNotBlank() || maxAmount.isNotBlank()
}

class SearchViewModel(repository: ExpenseRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        repository.allExpenses, _filterState
    ) { expenses, filter ->
        expenses.filter { expense ->
            matchesQuery(expense, filter.query) &&
                    matchesCategory(expense, filter.selectedCategories) &&
                    matchesDateRange(expense, filter.dateRange) &&
                    matchesAmountRange(expense, filter.minAmount, filter.maxAmount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(query: String) = _filterState.update { it.copy(query = query) }

    fun toggleCategory(category: String) = _filterState.update {
        val current = it.selectedCategories
        it.copy(selectedCategories = if (category in current) current - category else current + category)
    }

    fun onDateRangeChange(startMillis: Long?, endMillis: Long?) =
        _filterState.update { it.copy(dateRange = DateRangeFilter(startMillis, endMillis)) }

    fun onMinAmountChange(value: String) = _filterState.update { it.copy(minAmount = value) }
    fun onMaxAmountChange(value: String) = _filterState.update { it.copy(maxAmount = value) }

    fun clearFilters() = _filterState.update { SearchFilterState(query = it.query) }

    private fun matchesQuery(expense: Expense, query: String): Boolean =
        query.isBlank() || expense.title.contains(query, ignoreCase = true)

    private fun matchesCategory(expense: Expense, categories: Set<String>): Boolean =
        categories.isEmpty() || expense.category in categories

    private fun matchesDateRange(expense: Expense, range: DateRangeFilter): Boolean {
        if (range.startMillis != null && expense.dateMillis < range.startMillis) return false
        if (range.endMillis != null && expense.dateMillis > range.endMillis) return false
        return true
    }

    private fun matchesAmountRange(expense: Expense, min: String, max: String): Boolean {
        min.toDoubleOrNull()?.let { if (expense.amount < it) return false }
        max.toDoubleOrNull()?.let { if (expense.amount > it) return false }
        return true
    }

    companion object {
        fun factory(repository: ExpenseRepository) = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}