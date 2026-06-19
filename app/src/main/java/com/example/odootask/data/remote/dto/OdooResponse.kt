package com.example.odootask.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Generic JSON-RPC response envelope. Exactly one of [result] / [error] is present. */
data class OdooResponse<T>(
    @SerializedName("result") val result: T?,
    @SerializedName("error") val error: OdooError?,
)

data class OdooError(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: OdooErrorData?,
)

data class OdooErrorData(
    @SerializedName("name") val name: String?,
    @SerializedName("message") val message: String?,
)
