package com.skyplus.billdesk.sync

import java.util.UUID

/**
 * The id of a bill is generated on the device, before it is ever sent anywhere.
 *
 * That single decision is what makes sync safe: if the network dies after the
 * server saved the bill but before the app heard back, the app retries with the
 * same id and the server ignores the duplicate. A server-generated id would give
 * the restaurant two bills for one table.
 */
object IdempotencyKey {

    private val UUID_V4 = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    /** A fresh key for a new bill, stock movement or any append-only row. */
    fun newKey(): String = UUID.randomUUID().toString()

    /** Rejects anything that is not a canonical lowercase UUID v4. */
    fun isValid(key: String): Boolean = UUID_V4.matches(key)

    fun requireValid(key: String): String {
        require(isValid(key)) { "Not a valid idempotency key: $key" }
        return key
    }
}
