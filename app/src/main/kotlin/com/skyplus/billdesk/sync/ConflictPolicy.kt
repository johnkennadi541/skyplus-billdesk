package com.skyplus.billdesk.sync

/** How the server settles a disagreement between two devices. */
enum class ConflictRule {
    /**
     * Append-only and idempotent: a row is inserted once, keyed by the id the
     * device generated. Re-sending it changes nothing. Used for money.
     */
    APPEND_ONLY,

    /**
     * The newest `updatedAt` wins. Acceptable for menu and settings, where the
     * last person who edited it meant it.
     */
    LAST_WRITE_WINS,

    /**
     * One way only: a bill can become void, never un-void, and the first void
     * wins. Two devices voiding the same bill must not double the refund.
     */
    MONOTONIC_VOID,

    /**
     * Derived on the server from append-only movements; the device may not push
     * a quantity at all. This is what stops two tills from each subtracting the
     * same usage and ending at a wrong number.
     */
    SERVER_DERIVED
}

/**
 * One rule per entity, decided once and tested.
 *
 * The rule that matters most: money is never merged, never overwritten and never
 * averaged. A bill is an event, not a row to be edited, so two devices that both
 * saw it cannot damage it.
 */
object ConflictPolicy {

    private val rules: Map<SyncEntity, ConflictRule> = mapOf(
        SyncEntity.BILL to ConflictRule.APPEND_ONLY,
        SyncEntity.BILL_ITEM to ConflictRule.APPEND_ONLY,
        SyncEntity.BILL_VOID to ConflictRule.MONOTONIC_VOID,
        SyncEntity.MENU_ITEM to ConflictRule.LAST_WRITE_WINS,
        SyncEntity.CATEGORY to ConflictRule.LAST_WRITE_WINS,
        SyncEntity.INVENTORY_ITEM to ConflictRule.SERVER_DERIVED,
        SyncEntity.STOCK_MOVEMENT to ConflictRule.APPEND_ONLY,
        SyncEntity.SETTINGS to ConflictRule.LAST_WRITE_WINS,
        SyncEntity.MEMBER to ConflictRule.SERVER_DERIVED
    )

    fun ruleFor(entity: SyncEntity): ConflictRule =
        rules[entity] ?: error("No conflict rule for $entity")

    /** True when the device may never overwrite an existing server row. */
    fun isAppendOnly(entity: SyncEntity): Boolean = ruleFor(entity) == ConflictRule.APPEND_ONLY

    /** True when the device must not push a value at all; the server derives it. */
    fun isServerDerived(entity: SyncEntity): Boolean = ruleFor(entity) == ConflictRule.SERVER_DERIVED

    /**
     * True when an older write must be rejected in favour of the newer one.
     * Equal timestamps keep the existing row: a tie is not a reason to overwrite.
     */
    fun newerWins(entity: SyncEntity, incomingUpdatedAt: Long, existingUpdatedAt: Long): Boolean =
        when (ruleFor(entity)) {
            ConflictRule.LAST_WRITE_WINS -> incomingUpdatedAt > existingUpdatedAt
            ConflictRule.APPEND_ONLY,
            ConflictRule.MONOTONIC_VOID,
            ConflictRule.SERVER_DERIVED -> false
        }
}
