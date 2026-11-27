package com.skyplus.billdesk.data.entity

/** The active table lease remains ephemeral state, separate from append-only business events. */
data class TableLeaseSnapshot(val tableId: String, val ownerDeviceId: String, val expiresAtEpochMillis: Long)
