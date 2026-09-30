package com.varshith.expensetracker.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {

    private fun line(text: String, y: Int, x: Int = 0) =
        OcrLine(text = text, top = y, bottom = y + 20, left = x, right = x + 100)

    @Test fun simpleTotalWithDollarSignSplitAcrossColumns() {
        val lines = listOf(
            line("LOREM SHOP", 10),
            line("1: 0275  item one", 50), line("2.99", 50, x = 300),
            line("6: 9463  item two", 200), line("5.10", 200, x = 300),
            line("TOTAL", 300), line("$34.50", 300, x = 300)
        )
        assertEquals(34.50, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun grandTotalAfterSubtotalAndTaxPicksBottomMostMatch() {
        val lines = listOf(
            line("Subtotal", 200), line("100.00", 200, x = 300),
            line("Tax", 230), line("8.00", 230, x = 300),
            line("Grand Total", 260), line("108.00", 260, x = 300)
        )
        assertEquals(108.00, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun netPayableKeywordVariantOnSingleLine() {
        val lines = listOf(line("Net Payable  Rs. 450.00", 100))
        assertEquals(450.00, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun balanceDueKeywordVariantSplitAcrossColumns() {
        val lines = listOf(line("Balance Due", 100), line("\u20B9275.00", 100, x = 300))
        assertEquals(275.00, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun commaAsDecimalSeparator() {
        val lines = listOf(line("TOTAL  34,50", 100))
        assertEquals(34.50, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun commaDecimalWithThousandsDotSeparator() {
        val lines = listOf(line("TOTAL  1.234,50", 100))
        assertEquals(1234.50, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun noTotalKeywordFallsBackToLargestBareDecimalAmount() {
        val lines = listOf(line("Thank you", 400), line("450.00", 420))
        assertEquals(450.00, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun itemCodeIsNeverMistakenForTheAmount() {
        val lines = listOf(
            line("6: 9463  item two", 200), line("5.10", 200, x = 300)
        )
        assertEquals(5.10, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun ocrDigitLetterConfusionCorrectedAsLastResort() {
        val lines = listOf(line("TOTAL  5OO.OO", 100))
        assertEquals(500.00, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun fourDigitTotalWithoutCommaSeparatorIsNotTruncated() {
        val lines = listOf(
            line("SUBTOTAL", 2211, x = 168),
            line("TAX (8. 875%)", 2329, x = 165),
            line("TOTAL", 2499, x = 163),
            line("$1437.00", 2202, x = 1380),
            line("$127.53", 2330, x = 1426),
            line("$1564.53", 2497, x = 1386)
        )
        assertEquals(1564.53, ReceiptParser.parse(lines).amount!!, 0.001)
    }

    @Test fun noAmountAnywhereOnReceiptReturnsNull() {
        val lines = listOf(line("Thank you for shopping", 100), line("Visit again", 130))
        assertNull(ReceiptParser.parse(lines).amount)
    }

    @Test fun titleExtractionSkipsGstinAndInvoiceHeaderLines() {
        val lines = listOf(
            line("TAX INVOICE", 5),
            line("GSTIN: 29ABCDE1234F1Z5", 25),
            line("Cafe Coffee Day", 50)
        )
        assertEquals("Cafe Coffee Day", ReceiptParser.parse(lines).title)
    }
}