package com.skyplus.billdesk.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    private val record = PinHasher.hash("1947", iterations = 10_000)

    @Test
    fun theRecordHasFourParts() {
        assertEquals(4, record.split("$").size)
    }

    @Test
    fun theRecordIsWellFormed() {
        assertTrue(PinHasher.isWellFormed(record))
    }

    @Test
    fun theCorrectPinVerifies() {
        assertTrue(PinHasher.verify("1947", record))
    }

    @Test
    fun aWrongPinIsRejected() {
        assertFalse(PinHasher.verify("1948", record))
        assertFalse(PinHasher.verify("", record))
        assertFalse(PinHasher.verify("19470", record))
    }

    @Test
    fun thePinItselfIsNotInTheRecord() {
        assertFalse(record.contains("1947"))
    }

    @Test
    fun theSamePinTwiceGivesDifferentRecords() {
        val other = PinHasher.hash("1947", iterations = 10_000)
        assertNotEquals(record, other)
    }

    @Test
    fun bothRecordsStillVerify() {
        val other = PinHasher.hash("1947", iterations = 10_000)
        assertTrue(PinHasher.verify("1947", other))
    }

    @Test
    fun aTamperedRecordIsRejected() {
        val parts = record.split("$")
        val tampered = parts[0] + "$" + parts[1] + "$" + parts[2] + "$" + "00".repeat(32)
        assertFalse(PinHasher.verify("1947", tampered))
    }

    @Test
    fun aMalformedRecordIsRejected() {
        assertFalse(PinHasher.verify("1947", "not-a-record"))
        assertFalse(PinHasher.isWellFormed("not-a-record"))
        assertFalse(PinHasher.isWellFormed("a" + "$" + "b" + "$" + "c" + "$" + "d"))
    }

    @Test
    fun theSaltIsNotReused() {
        val a = PinHasher.newSalt()
        val b = PinHasher.newSalt()
        assertFalse(a.contentEquals(b))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooFewIterationsAreRejected() {
        PinHasher.hash("1947", iterations = 1000)
    }
}
