package com.skyplus.billdesk.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The numeric pad a cashier uses while a customer waits.
 *
 * Big buttons, one hand, no keyboard: a full text field at a counter costs seconds
 * on every bill, and seconds are the reason the cashier stays on a PIN at all.
 */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { digit ->
                    Button(
                        onClick = { onDigit(digit) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        Text(text = digit.toString(), fontSize = 24.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Text(text = "DEL", fontSize = 18.sp)
            }
            Button(
                onClick = { onDigit('0') },
                enabled = enabled,
                modifier = Modifier
                    .weight(2f)
                    .height(64.dp)
            ) {
                Text(text = "0", fontSize = 24.sp)
            }
        }
    }
}

/** Bullet display of what has been typed so far. */
@Composable
fun PinDisplay(state: PinEntryState, modifier: Modifier = Modifier) {
    Text(
        text = state.display,
        fontSize = 28.sp,
        modifier = modifier.padding(vertical = 12.dp)
    )
}
