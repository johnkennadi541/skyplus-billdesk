package com.skyplus.billdesk.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvWriterTest {

    private val header = listOf("Bill", "Date", "Item", "Qty", "Amount")
    private val rows = listOf(
        listOf("B-0001", "25/11/2026 20:15", "Paneer Tikka", "2", "480.00"),
        listOf("B-0002", "25/11/2026 20:31", "Tea", "3", "90.00")
    )

    @Test
    fun startsWithTheByteOrderMark() {
        assertTrue(CsvWriter.write(header, rows).startsWith(CsvWriter.BOM))
    }

    @Test
    fun writesHeaderAndRows() {
        val csv = CsvWriter.write(header, rows, withHash = false)
        val lines = csv.removePrefix(CsvWriter.BOM).split("\r\n")
        assertEquals(3, lines.size)
        assertEquals("Bill,Date,Item,Qty,Amount", lines[0])
        assertEquals("B-0001,25/11/2026 20:15,Paneer Tikka,2,480.00", lines[1])
    }

    @Test
    fun quotesCellsContainingCommas() {
        assertEquals("\"Paneer Tikka, dry\"", CsvWriter.cell("Paneer Tikka, dry"))
    }

    @Test
    fun doublesInnerQuotes() {
        assertEquals("\"say \"\"extra\"\" spicy\"", CsvWriter.cell("say \"extra\" spicy"))
    }

    @Test
    fun quotesCellsContainingNewlines() {
        assertEquals("\"line1\nline2\"", CsvWriter.cell("line1\nline2"))
    }

    @Test
    fun leavesPlainCellsAlone() {
        assertEquals("Paneer Tikka", CsvWriter.cell("Paneer Tikka"))
        assertEquals("", CsvWriter.cell(""))
    }

    @Test
    fun formatsAmountsWithoutGroupingOrSymbol() {
        assertEquals("1234.50", CsvWriter.amount(123450))
        assertEquals("0.05", CsvWriter.amount(5))
        assertEquals("0.00", CsvWriter.amount(0))
        assertEquals("99999999.99", CsvWriter.amount(9999999999))
    }

    @Test
    fun formatsNegativeAmounts() {
        assertEquals("-1234.50", CsvWriter.amount(-123450))
    }

    @Test
    fun formatsTimestampsInIndianTime() {
        // 1795627500000 ms = 2026-11-25 17:25 UTC = 22:55 in India
        assertEquals("25/11/2026 22:55", CsvWriter.timestamp(1795627500000, "Asia/Kolkata"))
        assertEquals("25/11/2026 17:25", CsvWriter.timestamp(1795627500000, "UTC"))
    }

    @Test
    fun appendsAHashLine() {
        val csv = CsvWriter.write(header, rows)
        assertNotNull(CsvWriter.hashOf(csv))
        assertEquals(64, CsvWriter.hashOf(csv)!!.length)
    }

    @Test
    fun anUntouchedDocumentVerifies() {
        assertTrue(CsvWriter.isUnmodified(CsvWriter.write(header, rows)))
    }

    @Test
    fun anEditedAmountIsDetected() {
        val csv = CsvWriter.write(header, rows)
        val tampered = csv.replace("480.00", "4800.00")
        assertFalse(CsvWriter.isUnmodified(tampered))
    }

    @Test
    fun aDocumentWithoutAHashDoesNotVerify() {
        assertFalse(CsvWriter.isUnmodified(CsvWriter.write(header, rows, withHash = false)))
        assertNull(CsvWriter.hashOf(CsvWriter.write(header, rows, withHash = false)))
    }

    @Test
    fun theSameRowsAlwaysProduceTheSameHash() {
        assertEquals(
            CsvWriter.hashOf(CsvWriter.write(header, rows)),
            CsvWriter.hashOf(CsvWriter.write(header, rows))
        )
    }

    @Test
    fun aDifferentValueChangesTheHash() {
        val other = listOf(
            listOf("B-0001", "25/11/2026 20:15", "Paneer Tikka", "2", "481.00")
        )
        assertTrue(CsvWriter.hashOf(CsvWriter.write(header, rows)) != CsvWriter.hashOf(CsvWriter.write(header, other)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsARowWithTheWrongCellCount() {
        CsvWriter.write(header, listOf(listOf("B-0001", "25/11/2026")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEmptyHeader() {
        CsvWriter.write(emptyList(), emptyList())
    }
}
