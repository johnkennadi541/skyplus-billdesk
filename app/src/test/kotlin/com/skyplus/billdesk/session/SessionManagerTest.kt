package com.skyplus.billdesk.session

import com.skyplus.billdesk.auth.PinPolicy
import com.skyplus.billdesk.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private var clock = 1_000_000L
    private val store = InMemorySessionStore()
    private val manager = SessionManager(store) { clock }

    private fun setUpCashier(pin: String = "1947") {
        store.saveAccount(
            UserAccount(
                id = "cashier-1",
                role = UserRole.CASHIER,
                displayName = "Ravi",
                pinRecord = com.skyplus.billdesk.auth.PinHasher.hash(pin, iterations = 10_000)
            )
        )
    }

    @Test
    fun theDeviceLandsInAdminByDefault() {
        assertEquals(UserRole.ADMIN, manager.landingRole())
    }

    @Test
    fun aDeviceCanBeSetToLandInCashier() {
        manager.setLandingRole(UserRole.CASHIER)
        assertEquals(UserRole.CASHIER, manager.landingRole())
    }

    @Test
    fun aFreshDeviceNeedsSetup() {
        assertTrue(manager.needsSetup())
        setUpCashier()
        assertFalse(manager.needsSetup())
    }

    @Test
    fun theRightPinOpensTheTill() {
        setUpCashier()
        val result = manager.loginWithPin(UserRole.CASHIER, "1947")
        assertTrue(result is LoginResult.Success)
        assertEquals(UserRole.CASHIER, (result as LoginResult.Success).role)
    }

    @Test
    fun aWrongPinIsCountedNotThrown() {
        setUpCashier()
        val result = manager.loginWithPin(UserRole.CASHIER, "0000")
        assertTrue(result is LoginResult.WrongPin)
        assertEquals(PinPolicy.MAX_ATTEMPTS - 1, (result as LoginResult.WrongPin).attemptsLeft)
    }

    @Test
    fun aMissingProfileIsReported() {
        assertEquals(LoginResult.NoAccount, manager.loginWithPin(UserRole.ADMIN, "1234"))
    }

    @Test
    fun aProfileWithoutAPinSaysSo() {
        store.saveAccount(
            UserAccount(
                id = "admin-1",
                role = UserRole.ADMIN,
                displayName = "Owner",
                email = "owner@skyplus.example"
            )
        )
        assertEquals(LoginResult.PinNotSet, manager.loginWithPin(UserRole.ADMIN, "1234"))
    }

    @Test
    fun fiveWrongPinsLockTheProfile() {
        setUpCashier()
        repeat(4) { manager.loginWithPin(UserRole.CASHIER, "0000") }
        val fifth = manager.loginWithPin(UserRole.CASHIER, "0000")
        assertTrue(fifth is LoginResult.Locked)
        assertTrue(manager.isLocked(UserRole.CASHIER))
    }

    @Test
    fun aLockedProfileRejectsEvenTheRightPin() {
        setUpCashier()
        repeat(5) { manager.loginWithPin(UserRole.CASHIER, "0000") }
        assertTrue(manager.loginWithPin(UserRole.CASHIER, "1947") is LoginResult.Locked)
    }

    @Test
    fun theLockExpiresOnItsOwn() {
        setUpCashier()
        repeat(5) { manager.loginWithPin(UserRole.CASHIER, "0000") }
        clock += PinPolicy.FIRST_LOCK_MILLIS
        assertFalse(manager.isLocked(UserRole.CASHIER))
        assertTrue(manager.loginWithPin(UserRole.CASHIER, "1947") is LoginResult.Success)
    }

    @Test
    fun aGoodPinAfterAFailureResetsTheCount() {
        setUpCashier()
        manager.loginWithPin(UserRole.CASHIER, "0000")
        manager.loginWithPin(UserRole.CASHIER, "1947")
        assertEquals(0, store.lockState(UserRole.CASHIER).failedAttempts)
    }

    @Test
    fun settingAPinStoresAHashNotThePin() {
        manager.setPin(UserRole.CASHIER, "1947")
        val record = store.accountFor(UserRole.CASHIER)?.pinRecord
        assertTrue(record != null)
        assertFalse(record!!.contains("1947"))
        assertTrue(manager.loginWithPin(UserRole.CASHIER, "1947") is LoginResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aThreeDigitCashierPinIsRejected() {
        manager.setPin(UserRole.CASHIER, "123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun aSevenDigitAdminPinIsRejected() {
        manager.setPin(UserRole.ADMIN, "1234567")
    }

    @Test
    fun changingAPinAlsoClearsTheLockout() {
        setUpCashier()
        repeat(5) { manager.loginWithPin(UserRole.CASHIER, "0000") }
        assertTrue(manager.isLocked(UserRole.CASHIER))
        manager.setPin(UserRole.CASHIER, "5555")
        assertFalse(manager.isLocked(UserRole.CASHIER))
        assertTrue(manager.loginWithPin(UserRole.CASHIER, "5555") is LoginResult.Success)
    }
}
