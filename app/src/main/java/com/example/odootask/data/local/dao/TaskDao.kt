package com.example.odootask.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.odootask.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): TaskEntity?

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    /** Atomically swap the cached set so observers see a single emission (no empty flicker). */
    @Transaction
    suspend fun replaceAll(tasks: List<TaskEntity>) {
        clear()
        upsertAll(tasks)
    }

    @Query("UPDATE tasks SET stageId = :stageId, stageName = :stageName WHERE id = :id")
    suspend fun updateStage(id: Int, stageId: Int, stageName: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}
