package com.skyplus.billdesk.kitchen

import kotlin.math.ceil

data class StationLoad(val pendingOrdersCount: Int, val avgPrepMinutes: Double, val activeStationStaff: Int) {
    init { require(pendingOrdersCount >= 0 && avgPrepMinutes >= 0.0 && activeStationStaff >= 0) }
}

data class OrderEta(val stationWaitMinutes: Map<String, Int>, val itemPrepMinutes: Int, val totalMinutes: Int)

object EtaCalculator {
    fun stationWaitMinutes(load: StationLoad): Int = ceil(
        load.pendingOrdersCount * load.avgPrepMinutes / maxOf(1, load.activeStationStaff)
    ).toInt()

    /** Longest relevant station queue plus the longest item's prep time. */
    fun forOrder(items: List<PersistedOrderLine>, loads: Map<String, StationLoad>, prepByMenuItem: Map<Long, Double> = emptyMap()): OrderEta {
        val relevant = items.map { it.stationId }.distinct().associateWith { station ->
            stationWaitMinutes(loads[station] ?: StationLoad(0, 0.0, 1))
        }
        val itemPrep = items.maxOfOrNull { ceil(prepByMenuItem[it.menuItemId] ?: 0.0).toInt() } ?: 0
        return OrderEta(relevant, itemPrep, (relevant.values.maxOrNull() ?: 0) + itemPrep)
    }
}
