package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.UserRole

/** How a profile proves who it is when the app opens. */
enum class LoginMethod {
    /** 4-digit PIN. Fast at the counter, works on every device, needs no network. */
    PIN,

    /**
     * Email and password, handled by Supabase Auth.
     *
     * Chosen over Sign in with Google on purpose: Google sign-in needs a Google
     * Cloud project, an OAuth client bound to a signing-key fingerprint, brand
     * verification, and Google Play services on the device. The owner does not want
     * Google Cloud or Firebase, and many Android POS terminals have no Play
     * services anyway, so a Google button would simply not exist on the till.
     */
    EMAIL_PASSWORD
}

/**
 * Which login a profile uses.
 *
 * Cashier: a 4-digit PIN. A password at 8pm on a busy Friday costs seconds on
 * every bill, and the cashier is not the person who needs a verified identity.
 *
 * Admin: email and password. The owner is the one who can change prices, void
 * bills and read every report, so that account is a real one - but it is our own
 * account, held in Supabase, not a Google account.
 */
object LoginPolicy {

    fun methodFor(role: UserRole): LoginMethod = when (role) {
        UserRole.CASHIER -> LoginMethod.PIN
        UserRole.ADMIN -> LoginMethod.EMAIL_PASSWORD
    }

    /** Only the admin profile has a cloud account at all. */
    fun hasCloudAccount(role: UserRole): Boolean = role == UserRole.ADMIN

    /** Always true: no device may ever be locked out of its own till. */
    const val PIN_FALLBACK_ALWAYS_AVAILABLE: Boolean = true
}
