package com.skyplus.billdesk.model

/**
 * The two operating profiles of the app. Exactly two, by product design.
 *
 * A user is not a person here - it is the profile currently driving the till.
 * Who did what is recorded separately in the audit fields on [Bill].
 */
enum class UserRole {
    CASHIER,
    ADMIN
}
