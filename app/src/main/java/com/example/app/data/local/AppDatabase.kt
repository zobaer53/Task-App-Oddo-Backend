package com.example.odootask.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.odootask.data.local.dao.TaskDao
import com.example.odootask.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val VERSION = 1
        const val NAME = "odoo_task.db"
    }
}
