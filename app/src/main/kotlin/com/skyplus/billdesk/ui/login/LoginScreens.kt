package com.skyplus.billdesk.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplus.billdesk.auth.LoginPolicy
import com.skyplus.billdesk.model.UserRole

/**
 * One entry point that decides which login this device shows.
 *
 * The decision is [LoginPolicy]'s, not the UI's: cashier always PIN, admin Google
 * when Play services exist, admin PIN when they do not. Keeping the decision out
 * of the composable is what makes it testable.
 */
@Composable
fun LoginRoute(
    role: UserRole,
    playServicesAvailable: Boolean,
    onPinSubmit: (String) -> Unit,
    onGoogleSignIn: () -> Unit,
    errorText: String? = null,
    enabled: Boolean = true
) {
    val method = LoginPolicy.methodFor(role, playServicesAvailable)

    if (method == com.skyplus.billdesk.auth.LoginMethod.GOOGLE) {
        AdminGoogleLoginScreen(
            onGoogleSignIn = onGoogleSignIn,
            onUsePinInstead = { onPinSubmit("") },
            errorText = errorText,
            enabled = enabled
        )
    } else {
        PinLoginScreen(
            role = role,
            title = if (role == UserRole.ADMIN) "Admin PIN" else "Cashier PIN",
            subtitle = if (role == UserRole.ADMIN && LoginPolicy.adminNeedsPinFallback(playServicesAvailable)) {
                "Google sign-in is not available on this device, so the admin uses a PIN here."
            } else {
                null
            },
            onSubmit = onPinSubmit,
            errorText = errorText,
            enabled = enabled
        )
    }
}

@Composable
fun PinLoginScreen(
    role: UserRole,
    title: String,
    onSubmit: (String) -> Unit,
    subtitle: String? = null,
    errorText: String? = null,
    enabled: Boolean = true
) {
    val maxLength = if (role == UserRole.ADMIN) 6 else 4
    var state by remember(role) { mutableStateOf(PinEntryState(maxLength = maxLength)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "SkyPlus BillDesk", fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = title, fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
        subtitle?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        PinDisplay(state)
        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
        PinPad(
            onDigit = { digit -> state = state.append(digit) },
            onBackspace = { state = state.backspace() },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onSubmit(state.digits)
                state = state.clear()
            },
            enabled = enabled && state.isComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
    }
}

@Composable
fun AdminGoogleLoginScreen(
    onGoogleSignIn: () -> Unit,
    onUsePinInstead: () -> Unit,
    errorText: String? = null,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "SkyPlus BillDesk", fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Admin sign in", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Only the owner's account can open the admin profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGoogleSignIn,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
                Spacer(Modifier.height(8.dp))
                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = onUsePinInstead, enabled = enabled) {
                    Text("Use the admin PIN instead")
                }
            }
        }
    }
}
