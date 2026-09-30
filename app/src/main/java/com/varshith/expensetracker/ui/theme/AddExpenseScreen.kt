package com.varshith.expensetracker.ui.theme

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.varshith.expensetracker.data.ExpenseRepository
import com.varshith.expensetracker.scan.OcrLine
import com.varshith.expensetracker.scan.ReceiptOcr
import com.varshith.expensetracker.scan.ReceiptParser
import com.varshith.expensetracker.scan.rememberDocumentScannerLauncher
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.mlkit.genai.prompt.Generation
import com.varshith.expensetracker.scan.ExtractionResult
import com.varshith.expensetracker.scan.GeminiNanoExpenseExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    repository: ExpenseRepository,
    onDismiss: () -> Unit
) {
    val viewModel: AddExpenseViewModel = viewModel(factory = AddExpenseViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Travel", "Shopping", "Bills", "Health", "Other")
    val generativeModel = remember { Generation.getClient() }

    val launchScanner = rememberDocumentScannerLauncher(
        onPagesScanned = { uris: List<Uri> ->
            viewModel.onScanStarted()
            scope.launch {
                try {
                    val allLines = mutableListOf<OcrLine>()
                    uris.forEach { allLines += ReceiptOcr.recognizeLines(context, it) }

                    val draft = when (val result = GeminiNanoExpenseExtractor.extract(generativeModel, allLines)) {
                        is ExtractionResult.Success -> result.draft
                        is ExtractionResult.Unavailable, is ExtractionResult.Failed ->
                            ReceiptParser.parse(allLines) // silent fallback — same UX either way
                    }
                    viewModel.applyScannedReceipt(draft)
                } catch (e: Exception) {
                    viewModel.onScanFailed()
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { launchScanner() },
                enabled = !uiState.isProcessingScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Receipt")
            }

            if (uiState.isProcessingScan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Reading receipt…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            uiState.scanErrorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (uiState.isPrefilledFromScan) {
                Text(
                    "Filled from scan — please review before saving",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            val amountValue = uiState.amount.toDoubleOrNull()
            when {
                uiState.amount.isNotEmpty() && amountValue == null ->
                    Text("Enter a valid number", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                amountValue != null && amountValue <= 0 ->
                    Text("Amount must be greater than 0", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.onCategoryChange(option)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formatDateForDisplay(uiState.dateMillis),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveExpense(onSaved = onDismiss) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDateForDisplay(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))