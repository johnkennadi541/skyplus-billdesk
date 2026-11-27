package com.skyplus.billdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["normalizedName"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val categoryId: Long = 0,
    val name: String,
    val normalizedName: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

@Entity(tableName = "menu_items", indices = [Index(value = ["code"], unique = true), Index("categoryId")])
data class MenuItemEntity(
    @PrimaryKey(autoGenerate = true) val menuItemId: Long = 0,
    val code: String,
    val name: String,
    val categoryId: Long,
    val pricePaise: Long,
    val taxPercent: Double,
    val isActive: Boolean = true
)

@Entity(tableName = "kitchen_stations", indices = [Index("language")])
data class KitchenStationEntity(
    @PrimaryKey val stationId: String,
    val name: String,
    /** Stable value from KitchenLanguage; kept as text to avoid a converter dependency. */
    val language: String = "ENGLISH",
    val isActive: Boolean = true
)

@Entity(
    tableName = "station_menu_mapping",
    primaryKeys = ["stationId", "menuItemId"],
    indices = [Index("menuItemId")]
)
data class StationMenuMappingEntity(val stationId: String, val menuItemId: Long)

@Entity(tableName = "menu_translations", primaryKeys = ["menuItemId", "language"])
data class MenuTranslationEntity(val menuItemId: Long, val language: String, val translatedName: String)

@Entity(tableName = "orders", indices = [Index("status"), Index("openedAtEpochMillis"), Index("tableLabel")])
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val orderType: String,
    val tableLabel: String?,
    val status: String = "OPEN",
    val openedAtEpochMillis: Long,
    val closedAtEpochMillis: Long? = null
)

@Entity(tableName = "order_items", indices = [Index("orderId"), Index("menuItemId"), Index("stationId")])
data class OrderItemEntity(
    @PrimaryKey val lineId: String,
    val orderId: String,
    val menuItemId: Long,
    val nameSnapshot: String,
    val quantity: Int,
    val unitPricePaise: Long,
    val taxPercent: Double,
    val stationId: String,
    val note: String = ""
)

@Entity(tableName = "kot_records", indices = [Index("orderId"), Index("stationId")])
data class KotRecordEntity(
    @PrimaryKey val kotId: String,
    val orderId: String,
    val stationId: String,
    val language: String,
    val createdAtEpochMillis: Long,
    val status: String = "CREATED"
)

@Entity(tableName = "kot_lines", indices = [Index("kotId")])
data class KotLineEntity(
    @PrimaryKey val kotLineId: String,
    val kotId: String,
    val sourceLineId: String,
    val menuItemId: Long,
    val quantity: Int,
    val printedName: String,
    val note: String
)

@Entity(tableName = "event_log", indices = [Index(value = ["deviceId", "seq"], unique = true), Index("eventType"), Index("timestampEpochMillis")])
data class EventLogEntity(
    @PrimaryKey val eventId: String,
    val deviceId: String,
    val seq: Long,
    val timestampEpochMillis: Long,
    val eventType: String,
    val payload: String
)
