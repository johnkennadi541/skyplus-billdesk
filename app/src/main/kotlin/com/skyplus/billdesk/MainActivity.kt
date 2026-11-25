package com.skyplus.billdesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyplus.billdesk.auth.LandingProfile
import com.skyplus.billdesk.platform.PackageManagerPlayServicesChecker
import com.skyplus.billdesk.ui.login.LoginRoute
import com.skyplus.billdesk.ui.theme.SkyPlusBillDeskTheme

/**
 * Entry point.
 *
 * Step 2 wires the login screens to a placeholder session. The real PIN check, the
 * Google sign-in and the billing screens arrive next; until then the placeholder
 * says plainly that the unlock is not connected, rather than pretending to work.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playServices = PackageManagerPlayServicesChecker(this)

        setContent {
            SkyPlusBillDeskTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppEntry(playServicesAvailable = playServices.isAvailable())
                }
            }
        }
    }
}

@Composable
private fun AppEntry(playServicesAvailable: Boolean) {
    var unlocked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (unlocked) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Admin session open. The billing screens are next.")
        }
        return
    }

    LoginRoute(
        role = LandingProfile.DEFAULT,
        playServicesAvailable = playServicesAvailable,
        onPinSubmit = { pin ->
            error = if (pin.isEmpty()) {
                "PIN entry is not connected yet."
            } else {
                "PIN check is not connected yet - it arrives with the session store."
            }
        },
        onGoogleSignIn = {
            error = "Google sign-in is not connected yet - it needs the Supabase project."
        },
        errorText = error
    )
}
