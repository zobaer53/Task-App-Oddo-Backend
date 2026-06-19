package com.example.odootask.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.odootask.data.local.dao.ItemDao
import com.example.odootask.data.local.entity.ItemEntity

@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        const val VERSION = 1
        const val NAME = "app.db"
    }
}
