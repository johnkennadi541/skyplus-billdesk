package com.skyplus.billdesk.session

import com.skyplus.billdesk.model.UserRole

/**
 * A profile that can open the till.
 *
 * [pinRecord] is a PBKDF2 record from [com.skyplus.billdesk.auth.PinHasher], never
 * a PIN. A cashier always has one; an admin has one too, because a POS terminal
 * with no network still has to be openable.
 */
data class UserAccount(
    val id: String,
    val role: UserRole,
    val displayName: String,
    val email: String? = null,
    val pinRecord: String? = null,
    val isActive: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "An account needs an id" }
        require(displayName.isNotBlank()) { "An account needs a display name" }
        require(role == UserRole.CASHIER || email != null) {
            "An admin account needs an email"
        }
    }

    fun canOpenWithPin(): Boolean = isActive && pinRecord != null
}
