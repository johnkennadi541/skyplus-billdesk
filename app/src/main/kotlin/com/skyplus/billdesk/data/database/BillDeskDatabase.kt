package com.skyplus.billdesk.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.JournalMode
import com.skyplus.billdesk.data.dao.EventSyncDao
import com.skyplus.billdesk.data.dao.PosDao
import com.skyplus.billdesk.data.dao.PrintQueueDao
import com.skyplus.billdesk.data.dao.PrinterProfileDao
import com.skyplus.billdesk.data.dao.TableLeaseDao
import com.skyplus.billdesk.data.entity.*

@Database(
    entities = [
        CategoryEntity::class, MenuItemEntity::class, KitchenStationEntity::class,
        StationMenuMappingEntity::class, MenuTranslationEntity::class,
        OrderEntity::class, OrderItemEntity::class, KotRecordEntity::class,
        KotLineEntity::class, EventLogEntity::class, PrintJobEntity::class,
        PrinterProfileEntity::class, TableEditLeaseEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class BillDeskDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao
    abstract fun eventSyncDao(): EventSyncDao
    abstract fun printQueueDao(): PrintQueueDao
    abstract fun printerProfileDao(): PrinterProfileDao
    abstract fun tableLeaseDao(): TableLeaseDao

    companion object {
        const val FILE_NAME = "skyplus-billdesk.db"
        fun open(context: Context): BillDeskDatabase = Room.databaseBuilder(
            context.applicationContext, BillDeskDatabase::class.java, FILE_NAME
        )
            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
