package com.varshith.expensetracker.scan

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

data class ParsedReceiptDraft(
    val title: String? = null,
    val amount: Double? = null,
    val category: String? = null,
    val date: Long? = null
)

object ReceiptParser {

    private val totalKeywords = listOf(
        "grand total", "total amount", "amount due", "net amount", "net payable",
        "amount payable", "balance due", "bill amount", "total payable",
        "to pay", "total"
    )
    private val ignoredTitleKeywords = listOf("receipt", "invoice", "gstin", "tax invoice", "bill no", "order no")

    // FIXED: added (?!\d) and (?!,\d) negative lookaheads. Without these,
    // this pattern could match just a PREFIX of a larger number — e.g. on
    // "1.234,50" (a European-format number using dot as a thousands
    // separator), \d+\.\d{1,2} would happily match only "1.23" and stop,
    // silently discarding the "4,50" that follows and producing a wrong
    // result. The lookaheads reject any match that's immediately followed
    // by more digits or a comma+digit, forcing the comma-decimal pass
    // (below) to correctly handle the full number instead.
    private val strictMoneyPattern = Regex(
        """(?:₹|rs\.?|inr|\$)?\s*(\d{1,3}(?:,\d{2,3})+\.\d{1,2}|\d+\.\d{1,2})(?!\d)(?!,\d)""",
        RegexOption.IGNORE_CASE
    )

    // Symmetric guard applied here too, for the same reason in reverse
    // (prevents matching only part of a dot-thousands-formatted number).
    private val commaDecimalPattern = Regex(
        """(?:₹|rs\.?|inr|\$|€)?\s*(\d{1,3}(?:\.\d{2,3})+,\d{1,2}|\d+,\d{1,2})(?!\d)(?!\.\d)""",
        RegexOption.IGNORE_CASE
    )

    private val looseMoneyToken = Regex(
        """[₹Rs\$]{0,3}\s*([0-9OoQIlLiSsBbZzGg]{1,10}\.[0-9OoQIlLiSsBbZzGg]{1,2})""",
        RegexOption.IGNORE_CASE
    )

    private val ocrDigitCorrections = mapOf(
        'O' to '0', 'o' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', 'i' to '1', 'L' to '1',
        'S' to '5', 's' to '5',
        'B' to '8', 'b' to '8',
        'Z' to '2', 'z' to '2',
        'G' to '6', 'g' to '6'
    )

    private val datePatterns = listOf(
        "dd/MM/yyyy" to Regex("""\b\d{1,2}/\d{1,2}/\d{4}\b"""),
        "dd-MM-yyyy" to Regex("""\b\d{1,2}-\d{1,2}-\d{4}\b"""),
        "yyyy-MM-dd" to Regex("""\b\d{4}-\d{1,2}-\d{1,2}\b""")
    )

    fun parse(ocrLines: List<OcrLine>): ParsedReceiptDraft {
        val texts = ocrLines.map { it.text }
        return ParsedReceiptDraft(
            title = extractTitle(texts),
            amount = extractAmount(ocrLines),
            category = null,
            date = extractDate(texts.joinToString("\n"))
        )
    }

    private fun extractAmount(ocrLines: List<OcrLine>): Double? {
        val keywordLines = ocrLines.filter { line ->
            val lowered = line.text.lowercase()
            totalKeywords.any { lowered.contains(it) }
        }

        keywordLines
            .mapNotNull { kw -> largestMoneyValue(kw.text)?.let { kw to it } }
            .maxByOrNull { it.first.top }
            ?.let { return it.second }

        val rowAlignedCandidates = keywordLines.mapNotNull { kw ->
            val tolerance = maxOf(kw.bottom - kw.top, 10)
            ocrLines
                .filter { it !== kw && abs(it.centerY - kw.centerY) <= tolerance }
                .mapNotNull { candidate -> largestMoneyValue(candidate.text)?.let { candidate.left to it } }
                .maxByOrNull { it.first }
                ?.let { kw to it.second }
        }
        rowAlignedCandidates.maxByOrNull { it.first.top }?.let { return it.second }

        ocrLines.mapNotNull { largestMoneyValue(it.text) }.maxOrNull()?.let { return it }

        keywordLines
            .mapNotNull { kw -> largestMoneyValueCommaDecimal(kw.text)?.let { kw to it } }
            .maxByOrNull { it.first.top }
            ?.let { return it.second }

        val commaRowAligned = keywordLines.mapNotNull { kw ->
            val tolerance = maxOf(kw.bottom - kw.top, 10)
            ocrLines
                .filter { it !== kw && abs(it.centerY - kw.centerY) <= tolerance }
                .mapNotNull { candidate -> largestMoneyValueCommaDecimal(candidate.text)?.let { candidate.left to it } }
                .maxByOrNull { it.first }
                ?.let { kw to it.second }
        }
        commaRowAligned.maxByOrNull { it.first.top }?.let { return it.second }

        ocrLines.mapNotNull { largestMoneyValueCommaDecimal(it.text) }.maxOrNull()?.let { return it }

        keywordLines
            .mapNotNull { kw -> largestMoneyValueTolerant(kw.text)?.let { kw to it } }
            .maxByOrNull { it.first.top }
            ?.let { return it.second }

        val tolerantRowAligned = keywordLines.mapNotNull { kw ->
            val tolerance = maxOf(kw.bottom - kw.top, 10)
            ocrLines
                .filter { it !== kw && abs(it.centerY - kw.centerY) <= tolerance }
                .mapNotNull { candidate -> largestMoneyValueTolerant(candidate.text)?.let { candidate.left to it } }
                .maxByOrNull { it.first }
                ?.let { kw to it.second }
        }
        tolerantRowAligned.maxByOrNull { it.first.top }?.let { return it.second }

        return ocrLines.mapNotNull { largestMoneyValueTolerant(it.text) }.maxOrNull()
    }

    private fun largestMoneyValue(text: String): Double? =
        strictMoneyPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .maxOrNull()

    private fun largestMoneyValueCommaDecimal(text: String): Double? =
        commaDecimalPattern.findAll(text)
            .mapNotNull {
                val normalized = it.groupValues[1].replace(".", "").replace(",", ".")
                normalized.toDoubleOrNull()
            }
            .maxOrNull()

    private fun largestMoneyValueTolerant(text: String): Double? =
        looseMoneyToken.findAll(text)
            .mapNotNull { match ->
                val rawToken = match.groupValues[1]
                val digitCount = rawToken.count { it.isDigit() }
                if (digitCount == 0) return@mapNotNull null
                rawToken.map { ch -> ocrDigitCorrections[ch] ?: ch }.joinToString("").toDoubleOrNull()
            }
            .maxOrNull()

    private fun extractDate(text: String): Long? {
        for ((format, regex) in datePatterns) {
            val match = regex.find(text) ?: continue
            val formatter = SimpleDateFormat(format, Locale.getDefault()).apply { isLenient = false }
            runCatching { formatter.parse(match.value) }.getOrNull()?.let { return it.time }
        }
        return null
    }

    private fun extractTitle(lines: List<String>): String? {
        for (line in lines.take(6)) {
            val trimmed = line.trim()
            val lowered = trimmed.lowercase()
            if (trimmed.length <= 2) continue
            if (!trimmed.any { it.isLetter() }) continue
            if (ignoredTitleKeywords.any { lowered.contains(it) }) continue
            return trimmed
        }
        return null
    }
}