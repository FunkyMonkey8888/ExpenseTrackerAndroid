package com.varshith.expensetracker.scan

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

/** Constrains Gemini Nano's output to your app's real category set. */
@Generable(description = "The best-fitting expense category for a receipt: Food, Travel, Shopping, Bills, Health, or Other")
data class CategoryGuess(
    @Guide(description = "One of: Food, Travel, Shopping, Bills, Health, Other")
    val category: String
)

@Generable(description = "Structured details extracted from a scanned receipt")
data class ScannedExpenseDraft(
    @Guide(description = "The merchant or vendor name printed on the receipt, e.g. 'Apple Store, Fifth Avenue'. Do not include the address or GST/tax ID lines.")
    val merchantName: String,

    @Guide(description = "The final total amount charged to the customer, as a plain number with no currency symbol or thousands separators, e.g. 1564.53")
    val totalAmount: Double,

    @Guide(description = "One of: Food, Travel, Shopping, Bills, Health, Other")
    val category: String,

    @Guide(description = "The transaction date in yyyy-MM-dd format. Use 0000-00-00 only if no date is visible anywhere in the text.")
    val transactionDate: String
)

fun ScannedExpenseDraft.toParsedReceiptDraft(): ParsedReceiptDraft {
    val knownCategories = setOf("Food", "Travel", "Shopping", "Bills", "Health", "Other")
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
        isLenient = false
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    val parsedDate = if (transactionDate == "0000-00-00") null
    else runCatching { formatter.parse(transactionDate)?.time }.getOrNull()

    return ParsedReceiptDraft(
        title = merchantName.trim().ifBlank { null },
        amount = totalAmount.takeIf { it > 0 },
        category = category.takeIf { it in knownCategories },
        date = parsedDate
    )
}