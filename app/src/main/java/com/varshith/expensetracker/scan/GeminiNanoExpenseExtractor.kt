package com.varshith.expensetracker.scan

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest

sealed class ExtractionResult {
    data class Success(val draft: ParsedReceiptDraft) : ExtractionResult()
    data class Unavailable(val reason: String) : ExtractionResult()
    data class Failed(val error: Throwable) : ExtractionResult()
}

object GeminiNanoExpenseExtractor {

    suspend fun isAvailable(model: GenerativeModel): Boolean =
        model.checkStatus() == FeatureStatus.AVAILABLE

    suspend fun extract(model: GenerativeModel, ocrLines: List<OcrLine>): ExtractionResult {
        val status = model.checkStatus()
        android.util.Log.d("GeminiNano", "FeatureStatus: $status")
        if (status != FeatureStatus.AVAILABLE) {
            return ExtractionResult.Unavailable(
                when (status) {
                    FeatureStatus.UNAVAILABLE -> "On-device AI isn't supported on this device."
                    FeatureStatus.DOWNLOADABLE -> "On-device AI model needs to be downloaded first."
                    FeatureStatus.DOWNLOADING -> "On-device AI model is still downloading."
                    else -> "On-device AI isn't ready right now."
                }
            )
        }

        val joinedText = ocrLines.joinToString("\n") { it.text }
        if (joinedText.isBlank()) return ExtractionResult.Unavailable("No text found on receipt.")

        val prompt = """
            You extract structured expense information from raw OCR text taken from
            a photographed receipt. The text may contain misread characters or
            out-of-order lines from the scan. Only use information actually present
            in the text; never invent a merchant name, amount, or date.

            Receipt text:
            $joinedText
        """.trimIndent()

        return try {
            val baseRequest = GenerateContentRequest.Builder(TextPart(prompt)).build()
            val typedRequest = generateTypedContentRequest(baseRequest, ScannedExpenseDraft::class)
            val typedResponse = model.generateContent(typedRequest)
            val draft = typedResponse.candidates.firstOrNull()?.response
                ?: return ExtractionResult.Failed(IllegalStateException("Empty response from model"))
            ExtractionResult.Success(draft.toParsedReceiptDraft())
        } catch (e: Exception) {
            ExtractionResult.Failed(e)
        }
    }
}