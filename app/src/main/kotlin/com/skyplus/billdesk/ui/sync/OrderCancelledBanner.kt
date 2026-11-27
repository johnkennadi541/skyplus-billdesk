package com.skyplus.billdesk.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skyplus.billdesk.data.entity.EventLogEntity
import com.skyplus.billdesk.sync.IncomingEventPresentation

/** Mount this for each item returned by EventSyncDao.cancellations() on the receiving device. */
@Composable
fun OrderCancelledBanner(event: EventLogEntity, modifier: Modifier = Modifier) {
    if (!IncomingEventPresentation(event).isCancellation) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB3261E), contentColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ORDER CANCELLED", style = MaterialTheme.typography.titleMedium)
            Text("Order cancellation received from ${event.deviceId}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
