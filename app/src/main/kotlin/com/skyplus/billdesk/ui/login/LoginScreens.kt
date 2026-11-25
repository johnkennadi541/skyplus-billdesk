package com.skyplus.billdesk.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyplus.billdesk.auth.LoginMethod
import com.skyplus.billdesk.auth.LoginPolicy
import com.skyplus.billdesk.model.UserRole

/**
 * One entry point that decides which login this device shows.
 *
 * The decision belongs to [LoginPolicy], not to the UI: cashier PIN, admin email and
 * password. Keeping it out of the composable is what makes it testable.
 */
@Composable
fun LoginRoute(
    role: UserRole,
    onPinSubmit: (String) -> Unit,
    onEmailSubmit: (String, String) -> Unit,
    onForgotPassword: () -> Unit = {},
    errorText: String? = null,
    enabled: Boolean = true
) {
    when (LoginPolicy.methodFor(role)) {
        LoginMethod.PIN -> PinLoginScreen(
            role = role,
            title = "Cashier PIN",
            onSubmit = onPinSubmit,
            errorText = errorText,
            enabled = enabled
        )

        LoginMethod.EMAIL_PASSWORD -> AdminEmailLoginScreen(
            onSubmit = onEmailSubmit,
            onForgotPassword = onForgotPassword,
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

/**
 * Admin sign in with an email and password held in Supabase Auth.
 *
 * Deliberately not "Sign in with Google": that would need a Google Cloud project, an
 * OAuth client tied to a signing fingerprint, brand verification, and Play services
 * on the device - and the owner does not want Google Cloud or Firebase.
 */
@Composable
fun AdminEmailLoginScreen(
    onSubmit: (String, String) -> Unit,
    onForgotPassword: () -> Unit = {},
    errorText: String? = null,
    enabled: Boolean = true
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = enabled && email.contains("@") && password.length >= 6

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
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = enabled,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onSubmit(email, password) },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in")
                }
                TextButton(onClick = onForgotPassword, enabled = enabled) {
                    Text("Forgot password")
                }
            }
        }
    }
}
