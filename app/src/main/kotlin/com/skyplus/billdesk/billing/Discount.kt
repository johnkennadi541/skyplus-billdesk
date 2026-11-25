package com.skyplus.billdesk.billing

/** A discount is either a percentage of the subtotal or a flat amount. */
sealed interface Discount {

    data object None : Discount

    /** 0 to 100 percent of the subtotal. */
    data class Percent(val percent: Double) : Discount {
        init {
            require(percent >= 0.0 && percent <= 100.0) {
                "Discount percent must be between 0 and 100, was $percent"
            }
        }
    }

    /** A flat amount in paise. Capped at the subtotal, never below zero. */
    data class Flat(val amountPaise: Long) : Discount {
        init {
            require(amountPaise >= 0) { "A flat discount must not be negative" }
        }
    }

    /** True when a cashier would need an admin PIN under the current policy. */
    fun percentOf(subtotalPaise: Long): Double = when (this) {
        None -> 0.0
        is Percent -> percent
        is Flat -> if (subtotalPaise <= 0) 0.0 else amountPaise * 100.0 / subtotalPaise
    }
}
