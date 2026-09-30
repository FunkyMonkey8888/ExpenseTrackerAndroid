@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.varshith.expensetracker.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.varshith.expensetracker.data.ExpenseRepository
import com.varshith.expensetracker.ui.components.ExpenseListItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ALL_CATEGORIES = listOf("Food", "Travel", "Shopping", "Bills", "Health", "Other")

@Composable
fun SearchScreen(
    repository: ExpenseRepository,
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit
) {
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(repository))
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val results by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    var showFilterPanel by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = filterState.query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = { Text("Search expenses…") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (filterState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Filters",
                                tint = if (filterState.isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                if (showFilterPanel) {
                    FilterPanel(
                        filterState = filterState,
                        allCategories = ALL_CATEGORIES,
                        onToggleCategory = viewModel::toggleCategory,
                        onPickDateRange = { showDatePicker = true },
                        onMinAmountChange = viewModel::onMinAmountChange,
                        onMaxAmountChange = viewModel::onMaxAmountChange,
                        onClear = viewModel::clearFilters
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "${results.size} result${if (results.size == 1) "" else "s"}" +
                        if (results.isNotEmpty()) "  \u00b7  ${currencyFormat.format(results.sumOf { it.amount })}" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp, 8.dp)
            )
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No expenses match your search/filters", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { expense ->
                        ExpenseListItem(expense = expense, onClick = { onExpenseClick(expense.id) })
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filterState.dateRange.startMillis,
            initialSelectedEndDateMillis = filterState.dateRange.endMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDateRangeChange(rangeState.selectedStartDateMillis, rangeState.selectedEndDateMillis)
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = rangeState, modifier = Modifier.height(460.dp))
        }
    }
}

@Composable
private fun FilterPanel(
    filterState: SearchFilterState,
    allCategories: List<String>,
    onToggleCategory: (String) -> Unit,
    onPickDateRange: () -> Unit,
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Category", style = MaterialTheme.typography.labelSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                allCategories.forEach { category ->
                    FilterChip(
                        selected = category in filterState.selectedCategories,
                        onClick = { onToggleCategory(category) },
                        label = { Text(category) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = filterState.minAmount,
                    onValueChange = onMinAmountChange,
                    label = { Text("Min \u20B9") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = filterState.maxAmount,
                    onValueChange = onMaxAmountChange,
                    label = { Text("Max \u20B9") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            val dateLabel = remember(filterState.dateRange) {
                val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
                val s = filterState.dateRange.startMillis?.let { fmt.format(Date(it)) }
                val e = filterState.dateRange.endMillis?.let { fmt.format(Date(it)) }
                if (s != null && e != null) "$s \u2013 $e" else "Select date range"
            }
            AssistChip(onClick = onPickDateRange, label = { Text(dateLabel) })

            if (filterState.isFilterActive) {
                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) {
                    Text("Clear all filters")
                }
            }
        }
    }
}