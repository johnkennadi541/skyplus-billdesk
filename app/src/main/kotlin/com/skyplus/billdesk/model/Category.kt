package com.skyplus.billdesk.model

/** Starter, Main Course, Beverages, Combos - the restaurant's own grouping. */
data class Category(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank" }
    }
}
