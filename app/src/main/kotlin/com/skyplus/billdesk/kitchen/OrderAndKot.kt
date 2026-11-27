package com.skyplus.billdesk.kitchen

import com.skyplus.billdesk.model.OrderType
import java.util.UUID

/** Values are persisted as stable enum names; parcel maps to the existing TAKEAWAY type. */
enum class KitchenLanguage { TAMIL, HINDI, ENGLISH, NEPALI }
enum class OrderStatus { OPEN, KOT_SENT, BILLED, CANCELLED }

data class KitchenStation(
    val id: String,
    val name: String,
    val language: KitchenLanguage = KitchenLanguage.ENGLISH
) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}

data class StationMenuAssignment(val stationId: String, val menuItemId: Long) {
    init { require(stationId.isNotBlank() && menuItemId > 0) }
}

data class OrderLineDraft(
    val menuItemId: Long,
    val nameSnapshot: String,
    val quantity: Int,
    val unitPricePaise: Long,
    val taxPercent: Double,
    val stationId: String,
    val note: String = ""
) {
    init {
        require(menuItemId > 0 && nameSnapshot.isNotBlank() && stationId.isNotBlank())
        require(quantity > 0 && unitPricePaise >= 0 && taxPercent in 0.0..100.0)
    }
}

data class OrderDraft(
    val orderId: String = UUID.randomUUID().toString(),
    val type: OrderType,
    val tableLabel: String? = null,
    val openedAtEpochMillis: Long,
    val lines: List<OrderLineDraft>
) {
    init {
        require(orderId.isNotBlank() && lines.isNotEmpty())
        require(type != OrderType.DINE_IN || !tableLabel.isNullOrBlank()) {
    	        "Dine-in orders require a table label"
        }
        require(type == OrderType.DINE_IN || tableLabel == null) {
            "Only dine-in orders may have a table label"
        }
    }
}

data class KotLine(
    val sourceLineId: String,
    val menuItemId: Long,
    val quantity: Int,
    val printedName: String,
    val note: String
)

data class KotTicket(
    val kotId: String = UUID.randomUUID().toString(),
    val orderId: String,
    val stationId: String,
    val language: KitchenLanguage,
    val createdAtEpochMillis: Long,
    val lines: List<KotLine>
) {
    init { require(kotId.isNotBlank() && orderId.isNotBlank() && stationId.isNotBlank() && lines.isNotEmpty()) }
}

fun interface LocalMenuTranslationLookup {
    fun translation(menuItemId: Long, language: KitchenLanguage): String?
}

/** Pure local projection. Missing/blank translation always falls back to the snapshot name. */
class KotBuilder(private val lookup: LocalMenuTranslationLookup) {
    fun build(
        orderId: String,
        createdAtEpochMillis: Long,
        orderLines: List<PersistedOrderLine>,
        stations: Map<String, KitchenStation>
    ): List<KotTicket> = orderLines.groupBy { it.stationId }.toSortedMap().mapNotNull { (stationId, lines) ->
        val station = stations[stationId] ?: return@mapNotNull null
        KotTicket(
            orderId = orderId,
            stationId = stationId,
            language = station.language,
            createdAtEpochMillis = createdAtEpochMillis,
            lines = lines.map { line ->
                KotLine(
                    sourceLineId = line.lineId,
                    menuItemId = line.menuItemId,
                    quantity = line.quantity,
                    printedName = lookup.translation(line.menuItemId, station.language)
                        ?.takeIf { it.isNotBlank() } ?: line.nameSnapshot,
                    note = line.note.trim()
                )
            }
        )
    }
}

data class PersistedOrderLine(
    val lineId: String,
    val menuItemId: Long,
    val nameSnapshot: String,
    val quantity: Int,
    val unitPricePaise: Long,
    val taxPercent: Double,
    val stationId: String,
    val note: String
)
