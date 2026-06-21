package com.example.odootask.data.remote.dto

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

/**
 * The Odoo many2one/false quirk is the single trickiest mapping in the app and broke before
 * (phase 12). [TaskDto.toEntity] must resolve `stage_id = [id, "name"]` or `false`, and
 * `date_deadline = "yyyy-MM-dd"` or `false`, into a flat [com.example.odootask.data.local.entity.TaskEntity].
 *
 * These cover both the direct mapper call and the realistic path through Gson, which decodes the
 * many2one id as a [Double] — the exact shape the mapper has to defend against.
 */
class TaskDtoMapperTest {

    private val gson = Gson()

    @Test
    fun `stage_id array resolves to id and name`() {
        val entity = TaskDto(
            id = 1,
            name = "Task A",
            stageId = listOf(4.0, "In Progress"),
            dateDeadline = "2026-07-01",
        ).toEntity()

        assertThat(entity.stageId).isEqualTo(4)
        assertThat(entity.stageName).isEqualTo("In Progress")
        assertThat(entity.dateDeadline).isEqualTo("2026-07-01")
    }

    @Test
    fun `stage_id false resolves to zero and empty name`() {
        val entity = TaskDto(id = 2, name = "Task B", stageId = false, dateDeadline = false).toEntity()

        assertThat(entity.stageId).isEqualTo(0)
        assertThat(entity.stageName).isEmpty()
    }

    @Test
    fun `date_deadline false maps to null`() {
        val entity = TaskDto(id = 3, name = "Task C", stageId = false, dateDeadline = false).toEntity()

        assertThat(entity.dateDeadline).isNull()
    }

    @Test
    fun `malformed stage_id array degrades gracefully`() {
        // Missing name element: id resolves, name falls back to empty.
        val entity = TaskDto(id = 4, name = "Task D", stageId = listOf(7.0), dateDeadline = null).toEntity()

        assertThat(entity.stageId).isEqualTo(7)
        assertThat(entity.stageName).isEmpty()
    }

    @Test
    fun `parsed from Odoo json with array stage maps correctly`() {
        val json = """[{"id":10,"name":"Real Task","stage_id":[4,"Done"],"date_deadline":"2026-08-15"}]"""

        val entity = gson.fromJson(json, Array<TaskDto>::class.java).first().toEntity()

        assertThat(entity.id).isEqualTo(10)
        assertThat(entity.name).isEqualTo("Real Task")
        assertThat(entity.stageId).isEqualTo(4)
        assertThat(entity.stageName).isEqualTo("Done")
        assertThat(entity.dateDeadline).isEqualTo("2026-08-15")
    }

    @Test
    fun `parsed from Odoo json with false fields maps to defaults`() {
        val json = """[{"id":11,"name":"Empty Task","stage_id":false,"date_deadline":false}]"""

        val entity = gson.fromJson(json, Array<TaskDto>::class.java).first().toEntity()

        assertThat(entity.stageId).isEqualTo(0)
        assertThat(entity.stageName).isEmpty()
        assertThat(entity.dateDeadline).isNull()
    }
}
