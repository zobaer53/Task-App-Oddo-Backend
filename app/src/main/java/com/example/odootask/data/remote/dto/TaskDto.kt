package com.example.odootask.data.remote.dto

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
