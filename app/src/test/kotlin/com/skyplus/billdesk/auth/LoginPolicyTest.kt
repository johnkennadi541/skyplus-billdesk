package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPolicyTest {

    @Test
    fun cashierAlwaysUsesAPin() {
        assertEquals(LoginMethod.PIN, LoginPolicy.methodFor(UserRole.CASHIER, playServicesAvailable = true))
        assertEquals(LoginMethod.PIN, LoginPolicy.methodFor(UserRole.CASHIER, playServicesAvailable = false))
        assertFalse(LoginPolicy.googleAllowedFor(UserRole.CASHIER))
    }

    @Test
    fun adminUsesGoogleWhenPlayServicesArePresent() {
        assertEquals(LoginMethod.GOOGLE, LoginPolicy.methodFor(UserRole.ADMIN, playServicesAvailable = true))
        assertTrue(LoginPolicy.googleAllowedFor(UserRole.ADMIN))
        assertFalse(LoginPolicy.adminNeedsPinFallback(playServicesAvailable = true))
    }

    @Test
    fun adminFallsBackToPinOnARealPosTerminal() {
        assertEquals(LoginMethod.PIN, LoginPolicy.methodFor(UserRole.ADMIN, playServicesAvailable = false))
        assertTrue(LoginPolicy.adminNeedsPinFallback(playServicesAvailable = false))
    }

    @Test
    fun aPinFallbackAlwaysExists() {
        assertTrue(LoginPolicy.PIN_FALLBACK_ALWAYS_AVAILABLE)
    }
}
