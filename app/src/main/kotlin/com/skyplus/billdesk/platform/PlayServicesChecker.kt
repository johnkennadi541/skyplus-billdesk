package com.skyplus.billdesk.platform

import android.content.Context
import android.content.pm.PackageManager

/**
 * Does this device have Google Play services?
 *
 * Sign in with Google runs through Play services, and Google's own guidance is to
 * check before calling any Google API. Many Android POS terminals ship without the
 * Play Store, so this check decides whether the admin sees a Google button or a PIN.
 *
 * An interface, not a static call, so the login flow can be tested with both
 * answers on a machine that has neither.
 */
fun interface PlayServicesChecker {
    fun isAvailable(): Boolean
}

/**
 * Checks for the Play services package itself, which needs no Google dependency in
 * the app. When the Supabase / Google Auth SDK is added, swap this for
 * `GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)`.
 */
class PackageManagerPlayServicesChecker(private val context: Context) : PlayServicesChecker {

    override fun isAvailable(): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getPackageInfo(GMS_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private companion object {
        const val GMS_PACKAGE = "com.google.android.gms"
    }
}
