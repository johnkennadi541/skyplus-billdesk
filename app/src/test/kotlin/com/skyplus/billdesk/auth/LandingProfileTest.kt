package com.skyplus.billdesk.auth

import com.skyplus.billdesk.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingProfileTest {

    @Test
    fun appLandsInAdminWhenTheDeviceHasNoChoice() {
        assertEquals(UserRole.ADMIN, LandingProfile.roleFor(null))
        assertEquals(UserRole.ADMIN, LandingProfile.DEFAULT)
    }

    @Test
    fun aDeviceCanBeSetToLandInCashier() {
        assertEquals(UserRole.CASHIER, LandingProfile.roleFor(UserRole.CASHIER))
        assertFalse(LandingProfile.isAdminLanding(UserRole.CASHIER))
        assertTrue(LandingProfile.isAdminLanding(null))
    }

    @Test
    fun aPinIsRequiredOnEveryLaunch() {
        assertTrue(LandingProfile.PIN_REQUIRED_ON_LAUNCH)
    }
}
