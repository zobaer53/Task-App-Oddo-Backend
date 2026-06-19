package com.example.odootask.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Envelope for every Odoo JSON-RPC call. [params] is typed as [Any] (not generic):
 * Retrofit rejects wildcard `@Body` types, and Gson serializes by the concrete
 * runtime type anyway, so a [CommonParams] / [ObjectParams] / [DbParams] instance
 * passed here is serialized correctly.
 */
data class OdooRequest(
    @SerializedName("jsonrpc") val jsonrpc: String = "2.0",
    @SerializedName("method") val method: String = "call",
    @SerializedName("id") val id: Int = 1,
    @SerializedName("params") val params: Any,
)

/** params for the `common` service — used to authenticate and obtain a uid. */
data class CommonParams(
    @SerializedName("service") val service: String = "common",
    @SerializedName("method") val method: String = "authenticate",
    @SerializedName("args") val args: List<Any>,
)

/** params for the `object` service — used for all model CRUD via `execute_kw`. */
data class ObjectParams(
    @SerializedName("service") val service: String = "object",
    @SerializedName("method") val method: String = "execute_kw",
    @SerializedName("args") val args: List<Any>,
)

/** params for the `db` service — used to list databases for auto-detection. */
data class DbParams(
    @SerializedName("service") val service: String = "db",
    @SerializedName("method") val method: String = "list",
    @SerializedName("args") val args: List<Any> = emptyList(),
)
