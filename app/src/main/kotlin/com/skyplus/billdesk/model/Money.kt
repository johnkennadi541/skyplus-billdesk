package com.skyplus.billdesk.model

import kotlin.math.abs

/**
 * All money in this app is Long paise. Never Double, never Float.
 *
 * A bill that is off by one paisa is a bill the cashier cannot defend, so
 * conversion and formatting live here and are unit tested.
 */
object Money {

    const val PAISE_PER_RUPEE: Long = 100L

    private val RUPEE_INPUT = Regex("\\d+(\\.\\d{1,2})?")

    /** "12.5" -> 1250. Rejects anything that is not a plain rupee amount. */
    fun rupeesToPaise(rupees: String): Long {
        val text = rupees.trim()
        require(RUPEE_INPUT.matches(text)) { "Not a valid rupee amount: $rupees" }
        val parts = text.split(".")
        val whole = parts[0].toLong() * PAISE_PER_RUPEE
        val fraction = if (parts.size == 2) parts[1].padEnd(2, '0').toLong() else 0L
        return whole + fraction
    }

    /** 123450 -> "1,234.50" (Indian grouping). */
    fun format(paise: Long): String {
        val negative = paise < 0
        val value = abs(paise)
        val rupees = value / PAISE_PER_RUPEE
        val fraction = value % PAISE_PER_RUPEE
        val body = groupIndian(rupees.toString()) + "." +
            fraction.toString().padStart(2, '0')
        return if (negative) "-$body" else body
    }

    /** 123450 -> "Rs 1,234.50" - for bill text where a glyph may not render. */
    fun formatWithPrefix(paise: Long): String = "Rs " + format(paise)

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val groups = mutableListOf<String>()
        while (rest.length > 2) {
            groups.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) groups.add(0, rest)
        return groups.joinToString(",") + "," + last3
    }
}
