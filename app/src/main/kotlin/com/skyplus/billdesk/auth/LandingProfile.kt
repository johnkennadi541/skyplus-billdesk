package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.UserRole

/**
 * Which profile the app opens in.
 *
 * The owner's device lands in Admin; a cashier's tablet can be set to land in
 * Cashier instead. The choice is stored per device.
 *
 * Landing in Admin is not the same as skipping the PIN. Every fresh app start
 * asks for the Admin PIN once - there is deliberately no "stay admin all day"
 * option, because an unattended admin till can change prices and void bills.
 */
object LandingProfile {

    /** Default landing profile when the device has no stored choice. */
    val DEFAULT: UserRole = UserRole.ADMIN

    /** A PIN is asked on every fresh app start, whichever profile it lands in. */
    const val PIN_REQUIRED_ON_LAUNCH: Boolean = true

    fun roleFor(configured: UserRole?): UserRole = configured ?: DEFAULT

    fun isAdminLanding(configured: UserRole?): Boolean = roleFor(configured) == UserRole.ADMIN
}
