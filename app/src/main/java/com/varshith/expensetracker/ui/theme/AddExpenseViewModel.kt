package com.varshith.expensetracker.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.varshith.expensetracker.data.ExpenseRepository
import com.varshith.expensetracker.scan.ParsedReceiptDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class AddExpenseUiState(
    val title: String = "",
    val amount: String = "",
    val category: String = "Food",
    val dateMillis: Long = System.currentTimeMillis(),
    val isProcessingScan: Boolean = false,
    val scanErrorMessage: String? = null,
    val isPrefilledFromScan: Boolean = false
) {
    val isValid: Boolean
        get() {
            val value = amount.toDoubleOrNull() ?: return false
            return title.isNotBlank() && value > 0 && category.isNotBlank()
        }
}

class AddExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onDateChange(value: Long) = _uiState.update { it.copy(dateMillis = value) }

    fun onScanStarted() = _uiState.update { it.copy(isProcessingScan = true, scanErrorMessage = null) }

    fun onScanFailed() = _uiState.update {
        it.copy(isProcessingScan = false, scanErrorMessage = "Couldn't read the receipt. Please enter details manually.")
    }

    fun applyScannedReceipt(draft: ParsedReceiptDraft) = _uiState.update { current ->
        current.copy(
            title = draft.title ?: current.title,
            amount = draft.amount?.let { String.format(Locale.US, "%.2f", it) } ?: current.amount,
            category = draft.category ?: current.category,
            dateMillis = draft.date ?: current.dateMillis,
            isProcessingScan = false,
            isPrefilledFromScan = true
        )
    }

    fun saveExpense(onSaved: () -> Unit) {
        val state = _uiState.value
        val amountValue = state.amount.toDoubleOrNull() ?: return
        viewModelScope.launch {
            repository.createExpense(
                title = state.title.trim(),
                amount = amountValue,
                category = state.category,
                dateMillis = state.dateMillis
            )
            onSaved()
        }
    }

    companion object {
        fun factory(repository: ExpenseRepository) = viewModelFactory {
            initializer { AddExpenseViewModel(repository) }
        }
    }
}