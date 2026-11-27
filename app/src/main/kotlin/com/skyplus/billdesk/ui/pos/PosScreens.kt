package com.skyplus.billdesk.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyplus.billdesk.kitchen.*
import com.skyplus.billdesk.model.Money
import com.skyplus.billdesk.report.EndOfDaySummary

data class MenuStationRow(val itemName: String, val category: String, val pricePaise: Long, val taxPercent: Double, val station: KitchenStation)

@Composable fun MenuStationSetupScreen(rows: List<MenuStationRow>, onAddItem: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Menu & Kitchen Stations", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onAddItem) { Text("Add menu item") }
        LazyColumn { items(rows) { row ->
            ListItem(headlineContent = { Text(row.itemName) }, supportingContent = { Text("${row.category} · ${row.station.name} · ${row.station.language}") }, trailingContent = { Text("${Money.formatWithPrefix(row.pricePaise)} · GST ${row.taxPercent}%") })
        } }
    }
}

@Composable fun TableOrderScreen(
    tableLabel: String,
    items: List<PersistedOrderLine>,
    stations: Map<String, KitchenStation>,
    loads: Map<String, StationLoad>,
    prepByMenuItem: Map<Long, Double> = emptyMap(),
    kotPreview: List<KotTicket>,
    cancelled: Boolean = false,
    onAddItem: () -> Unit,
    onSendKot: () -> Unit,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eta = EtaCalculator.forOrder(items, loads, prepByMenuItem)
    Column(modifier.padding(16.dp)) {
        Text(tableLabel, style = MaterialTheme.typography.headlineSmall)
        if (cancelled) Text("ORDER CANCELLED — DO NOT PREPARE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall)
        Text("Estimated wait: ${eta.totalMinutes} min")
        Button(onClick = onAddItem, enabled = !cancelled) { Text("Add item") }
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(items) { line -> ListItem(headlineContent = { Text("${line.quantity} × ${line.nameSnapshot}") }, supportingContent = { Text("${stations[line.stationId]?.name ?: line.stationId} · ${line.note}") }) }
            kotPreview.forEach { ticket ->
                item { Text("${ticket.stationId} · ${ticket.language} KOT preview", style = MaterialTheme.typography.titleSmall) }
                items(ticket.lines) { line -> item { Text("${line.quantity} × ${line.printedName}${if (line.note.isBlank()) "" else " — ${line.note}"}") } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSendKot, enabled = !cancelled) { Text("Send KOT") }
            OutlinedButton(onClick = onCancelOrder, enabled = !cancelled) { Text("Cancel order") }
        }
    }
}

@Composable fun BillingScreen(subtotalPaise: Long, gstPaise: Long, discountPaise: Long, totalPaise: Long, etaMinutes: Int, onConfirmPayment: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Billing", style = MaterialTheme.typography.headlineSmall)
        Text("Prep estimate: $etaMinutes min")
        Text("Subtotal ${Money.formatWithPrefix(subtotalPaise)}")
        Text("GST ${Money.formatWithPrefix(gstPaise)}")
        Text("Discount −${Money.formatWithPrefix(discountPaise)}")
        Text("Total ${Money.formatWithPrefix(totalPaise)}", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onConfirmPayment) { Text("Take payment") }
    }
}

@Composable fun EndOfDayScreen(summary: EndOfDaySummary, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("End of Day", style = MaterialTheme.typography.headlineSmall)
        Text("Bills: ${summary.billCount} · Gross: ${Money.formatWithPrefix(summary.grossPaise)}")
        summary.tenderTotals.forEach { (mode, amount) -> Text("$mode: ${Money.formatWithPrefix(amount)}") }
        Text("Item sales", style = MaterialTheme.typography.titleMedium)
        summary.itemSales.forEach { Text("${it.name}: ${it.quantity} · ${Money.formatWithPrefix(it.grossPaise)}") }
        Text("Cancelled orders (${summary.cancellations.size})", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        summary.cancellations.forEach { Text("${it.orderId}: ${it.reason}", color = MaterialTheme.colorScheme.error) }
    }
}
