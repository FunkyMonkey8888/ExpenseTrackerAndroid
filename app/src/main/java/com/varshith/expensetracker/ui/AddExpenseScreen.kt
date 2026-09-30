package com.varshith.expensetracker.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.genai.prompt.Generation
import com.varshith.expensetracker.data.ExpenseRepository
import com.varshith.expensetracker.scan.ExtractionResult
import com.varshith.expensetracker.scan.GeminiNanoExpenseExtractor
import com.varshith.expensetracker.scan.OcrLine
import com.varshith.expensetracker.scan.ReceiptOcr
import com.varshith.expensetracker.scan.ReceiptParser
import com.varshith.expensetracker.scan.rememberDocumentScannerLauncher
import com.varshith.expensetracker.ui.theme.CategoryStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CategoryOption(val name: String)

private val CATEGORY_OPTIONS = listOf(
    CategoryOption("Food"), CategoryOption("Travel"), CategoryOption("Shopping"),
    CategoryOption("Bills"), CategoryOption("Health"), CategoryOption("Other")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    repository: ExpenseRepository,
    expenseId: Long? = null,
    onDone: () -> Unit
) {
    val viewModel: AddExpenseViewModel = viewModel(factory = AddExpenseViewModel.factory(repository, expenseId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val generativeModel = remember { Generation.getClient() }
    DisposableEffect(Unit) { onDispose { generativeModel.close() } }

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
                            ReceiptParser.parse(allLines)
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
                title = { Text(if (uiState.isEditMode) "Edit Expense" else "Add Expense", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onDone) { Text("Cancel") } },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete expense", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.saveExpense(onSaved = onDone) },
                        enabled = uiState.isValid,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isEditMode) "Update Expense" else "Save Expense", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoadingExisting) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ---- Big amount entry, the visual centerpiece ----
            AmountEntryCard(
                amount = uiState.amount,
                onAmountChange = viewModel::onAmountChange,
                category = uiState.category,
                errorText = when {
                    uiState.amount.isNotEmpty() && uiState.amount.toDoubleOrNull() == null -> "Enter a valid number"
                    uiState.amount.toDoubleOrNull()?.let { it <= 0 } == true -> "Amount must be greater than 0"
                    else -> null
                }
            )

            // ---- Scan receipt card ----
            ScanReceiptCard(
                isProcessing = uiState.isProcessingScan,
                errorMessage = uiState.scanErrorMessage,
                isPrefilled = uiState.isPrefilledFromScan,
                onScanClick = { launchScanner() }
            )

            // ---- Title ----
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("What was it for?") },
                placeholder = { Text("e.g. Groceries, Uber ride") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Category chips ----
            Column {
                Text("Category", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORY_OPTIONS.forEach { option ->
                        CategoryChip(
                            label = option.name,
                            selected = uiState.category == option.name,
                            onClick = { viewModel.onCategoryChange(option.name) }
                        )
                    }
                }
            }

            // ---- Date ----
            Column {
                Text("Date", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(formatDateForDisplay(uiState.dateMillis), style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this expense?") },
            text = { Text("This can't be undone from here.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteCurrentExpense(onDeleted = onDone)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AmountEntryCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    category: String,
    errorText: String?
) {
    val accentColor = CategoryStyle.colorFor(category)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = accentColor.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\u20B9",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
                BasicAmountField(value = amount, onValueChange = onAmountChange, accentColor = accentColor)
            }
            AnimatedVisibility(visible = errorText != null) {
                Text(
                    errorText.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BasicAmountField(value: String, onValueChange: (String) -> Unit, accentColor: Color) {
    val textStyle = TextStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        textAlign = TextAlign.Center
    )
    BasicTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 10) onValueChange(new) },
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = SolidColor(accentColor),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text("0.00", style = textStyle.copy(color = accentColor.copy(alpha = 0.35f)))
                }
                innerTextField()
            }
        },
        modifier = Modifier.widthIn(min = 80.dp)
    )
}

@Composable
private fun ScanReceiptCard(
    isProcessing: Boolean,
    errorMessage: String?,
    isPrefilled: Boolean,
    onScanClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableIfEnabled(enabled = !isProcessing, onClick = onScanClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isProcessing) "Reading receipt…" else "Scan a Receipt",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    when {
                        errorMessage != null -> errorMessage
                        isPrefilled -> "Filled from scan — please review"
                        else -> "Auto-fill details from a photo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error
                    else if (isPrefilled) Color(0xFF2E7D32)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Modifier.clickableIfEnabled(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.clickable(onClick = onClick) else this

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = CategoryStyle.colorFor(label)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                CategoryStyle.iconFor(label),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) Color.White else color
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White
        )
    )
}

private fun formatDateForDisplay(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))