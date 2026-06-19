package com.example.odootask.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Raw `project.project` record. */
data class ProjectDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
)
