package com.example.odootask.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Envelope for every Odoo JSON-RPC call. [params] is covariant so a request built
 * with a concrete params type (e.g. [ObjectParams]) can be passed where
 * `OdooRequest<Any>` is expected.
 */
data class OdooRequest<out T>(
    @SerializedName("jsonrpc") val jsonrpc: String = "2.0",
    @SerializedName("method") val method: String = "call",
    @SerializedName("id") val id: Int = 1,
    @SerializedName("params") val params: T,
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
