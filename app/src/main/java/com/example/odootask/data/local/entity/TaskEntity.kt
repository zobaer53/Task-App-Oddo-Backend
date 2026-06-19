package com.example.odootask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.odootask.domain.model.Task

/**
 * Cached `project.task` record. Mirrors the resolved shape of [TaskDto] so the
 * task list survives offline and process death. [stageName] is denormalised from
 * Odoo's many2one so rows render without a separate stage lookup.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val stageId: Int,
    val stageName: String,
    val dateDeadline: String?,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    name = name,
    stageId = stageId,
    stageName = stageName,
    dateDeadline = dateDeadline,
)
