package com.skyplus.billdesk.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinEntryStateTest {

    @Test
    fun startsEmpty() {
        val state = PinEntryState()
        assertEquals("", state.digits)
        assertFalse(state.isComplete)
        assertEquals("____", state.display)
    }

    @Test
    fun appendsDigits() {
        val state = PinEntryState().append('1').append('9').append('4').append('7')
        assertEquals("1947", state.digits)
        assertTrue(state.isComplete)
    }

    @Test
    fun ignoresLettersAndSymbols() {
        val state = PinEntryState().append('a').append('-').append('7')
        assertEquals("7", state.digits)
    }

    @Test
    fun ignoresInputOnceFull() {
        val state = PinEntryState(maxLength = 4).append("1947999")
        assertEquals("1947", state.digits)
    }

    @Test
    fun backspaceRemovesTheLastDigit() {
        assertEquals("19", PinEntryState().append("1947").backspace().backspace().digits)
    }

    @Test
    fun backspaceOnEmptyChangesNothing() {
        assertEquals("", PinEntryState().backspace().digits)
    }

    @Test
    fun clearEmptiesThePad() {
        assertEquals("", PinEntryState().append("1947").clear().digits)
    }

    @Test
    fun theDisplayShowsBulletsAndPlaceholders() {
        assertEquals("\u2022\u2022__", PinEntryState().append("12").display)
    }

    @Test
    fun anAdminPinCanBeSixDigits() {
        val state = PinEntryState(maxLength = 6)
        assertFalse(state.append("12345").isComplete)
        assertTrue(state.append("123456").isComplete)
    }

    @Test
    fun appendingAWholeStringWorks() {
        assertEquals("1947", PinEntryState().append("1947").digits)
    }
}
