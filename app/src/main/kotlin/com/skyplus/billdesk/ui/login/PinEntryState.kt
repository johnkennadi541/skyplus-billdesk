package com.skyplus.billdesk.ui.login

import com.skyplus.billdesk.auth.PinPolicy

/**
 * What is on the PIN pad right now. Pure, so the pad's behaviour is unit tested
 * instead of only being poked at on a device.
 */
data class PinEntryState(
    val digits: String = "",
    val maxLength: Int = PinPolicy.CASHIER_PIN_LENGTH
) {
    init {
        require(maxLength > 0) { "maxLength must be positive" }
    }

    val isComplete: Boolean get() = digits.length == maxLength

    /** Bullets for what has been typed, underscores for what is still needed. */
    val display: String
        get() = "\u2022".repeat(digits.length) + "_".repeat(maxLength - digits.length)

    /** Ignores anything that is not a digit, and ignores input once the PIN is full. */
    fun append(char: Char): PinEntryState =
        if (!char.isDigit() || isComplete) this else copy(digits = digits + char)

    fun append(text: String): PinEntryState =
        text.fold(this) { state, char -> state.append(char) }

    fun backspace(): PinEntryState =
        if (digits.isEmpty()) this else copy(digits = digits.dropLast(1))

    fun clear(): PinEntryState = copy(digits = "")
}
