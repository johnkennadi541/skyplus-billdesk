package com.skyplus.billdesk.model

/**
 * A sellable item. [pricePaise] is the price at the moment of billing; a bill
 * always keeps its own copy in [OrderItem], so a later price change never
 * rewrites history.
 */
data class MenuItem(
    val id: Long = 0,
    val code: String,
    val name: String,
    val categoryId: Long,
    val pricePaise: Long,
    val taxPercent: Double = 0.0,
    val isActive: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Menu item name must not be blank" }
        require(pricePaise >= 0) { "Price must not be negative" }
        require(taxPercent in 0.0..100.0) { "taxPercent must be between 0 and 100" }
    }

    /** Search and quick-add: matches the name or the code, case-insensitive. */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return name.contains(q, ignoreCase = true) || code.contains(q, ignoreCase = true)
    }
}
