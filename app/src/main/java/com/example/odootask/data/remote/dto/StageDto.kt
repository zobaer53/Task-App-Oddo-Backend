package com.example.odootask.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Raw `project.task.type` (stage) record. */
data class StageDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
)
