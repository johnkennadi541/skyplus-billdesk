package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPolicyTest {

    @Test
    fun cashierUsesAPin() {
        assertEquals(LoginMethod.PIN, LoginPolicy.methodFor(UserRole.CASHIER))
        assertFalse(LoginPolicy.hasCloudAccount(UserRole.CASHIER))
    }

    @Test
    fun adminUsesEmailAndPassword() {
        assertEquals(LoginMethod.EMAIL_PASSWORD, LoginPolicy.methodFor(UserRole.ADMIN))
        assertTrue(LoginPolicy.hasCloudAccount(UserRole.ADMIN))
    }

    @Test
    fun noProfileEverUsesGoogle() {
        UserRole.entries.forEach { role ->
            assertTrue(
                "$role must not need Google",
                LoginPolicy.methodFor(role) != LoginMethod.EMAIL_PASSWORD ||
                    role == UserRole.ADMIN
            )
        }
    }

    @Test
    fun aPinFallbackAlwaysExists() {
        assertTrue(LoginPolicy.PIN_FALLBACK_ALWAYS_AVAILABLE)
    }
}
