package com.example.odootask.data.remote.dto

import com.example.odootask.data.local.entity.TaskEntity
import com.google.gson.annotations.SerializedName

/**
 * Raw `project.task` record. Odoo encodes many2one and empty fields loosely:
 * [stageId] is `[id, "name"]` when set or `false` when empty, and [dateDeadline]
 * is `"yyyy-MM-dd"` or `false`. Both are typed [Any] and resolved during mapping.
 */
data class TaskDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("stage_id") val stageId: Any?,
    @SerializedName("date_deadline") val dateDeadline: Any?,
)

/**
 * Resolves Odoo's loose encoding into a flat [TaskEntity]: the many2one
 * `stage_id` arrives as `[id, "name"]` (Gson decodes the id as [Double]) or
 * `false`, and `date_deadline` as a string or `false`.
 */
fun TaskDto.toEntity(): TaskEntity {
    val (resolvedStageId, resolvedStageName) = when (val s = stageId) {
        is List<*> -> ((s.getOrNull(0) as? Double)?.toInt() ?: 0) to (s.getOrNull(1) as? String).orEmpty()
        else -> 0 to ""
    }
    return TaskEntity(
        id = id,
        name = name,
        stageId = resolvedStageId,
        stageName = resolvedStageName,
        dateDeadline = dateDeadline as? String,
    )
}
