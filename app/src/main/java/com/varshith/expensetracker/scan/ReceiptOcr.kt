package com.varshith.expensetracker.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class OcrLine(
    val text: String,
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int
) {
    val centerY: Int get() = (top + bottom) / 2
}

object ReceiptOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Preserves each line's bounding box so callers can detect same-row alignment. */
    suspend fun recognizeLines(context: Context, imageUri: Uri): List<OcrLine> {
        val image = InputImage.fromFilePath(context, imageUri)
        val result = recognizer.process(image).await()
        return result.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrLine(text = line.text, top = box.top, bottom = box.bottom, left = box.left, right = box.right)
            }
        }
    }
}
