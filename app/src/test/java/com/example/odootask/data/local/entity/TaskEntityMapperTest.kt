package com.example.odootask.data.local.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [TaskEntity.toDomain] is a straight field copy; this pins that contract. */
class TaskEntityMapperTest {

    @Test
    fun `toDomain copies every field`() {
        val entity = TaskEntity(
            id = 1,
            name = "Task",
            stageId = 4,
            stageName = "In Progress",
            dateDeadline = "2026-07-01",
        )

        val task = entity.toDomain()

        assertThat(task.id).isEqualTo(1)
        assertThat(task.name).isEqualTo("Task")
        assertThat(task.stageId).isEqualTo(4)
        assertThat(task.stageName).isEqualTo("In Progress")
        assertThat(task.dateDeadline).isEqualTo("2026-07-01")
    }

    @Test
    fun `toDomain preserves null deadline`() {
        val task = TaskEntity(id = 2, name = "No deadline", stageId = 0, stageName = "", dateDeadline = null).toDomain()

        assertThat(task.dateDeadline).isNull()
    }
}
