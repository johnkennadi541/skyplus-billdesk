package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.Permission
import com.skyplus.billdesk.model.UserRole

/** Raised when a role attempts an action it was not granted. */
class PermissionDeniedException(
    val role: UserRole,
    val permission: Permission
) : SecurityException("${role.name} is not allowed to perform ${permission.name}")

/**
 * The only place that answers "may this profile do this?".
 *
 * UI code asks; it never inspects the role itself. That keeps the matrix in one
 * file and makes the permission tests meaningful.
 */
object PermissionGuard {

    /**
     * Highest discount percentage a cashier may apply without an admin PIN.
     * Business policy - confirm the real number with the restaurant.
     */
    const val CASHIER_MAX_DISCOUNT_PERCENT: Double = 10.0

    fun can(role: UserRole, permission: Permission): Boolean =
        PermissionMatrix.isGranted(role, permission)

    /** Throws [PermissionDeniedException] when the role may not proceed. */
    fun require(role: UserRole, permission: Permission) {
        if (!can(role, permission)) throw PermissionDeniedException(role, permission)
    }

    /**
     * True when this discount needs an admin PIN on top of the current profile:
     * the discount is above the cashier limit and the role cannot exceed it.
     */
    fun requiresAdminPinForDiscount(role: UserRole, discountPercent: Double): Boolean {
        require(discountPercent in 0.0..100.0) {
            "discountPercent must be between 0 and 100, was $discountPercent"
        }
        return discountPercent > CASHIER_MAX_DISCOUNT_PERCENT &&
            !can(role, Permission.DISCOUNT_ABOVE_LIMIT)
    }
}
