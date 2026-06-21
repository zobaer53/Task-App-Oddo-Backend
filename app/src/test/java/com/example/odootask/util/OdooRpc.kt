package com.example.odootask.util

import com.example.odootask.data.remote.dto.OdooError
import com.example.odootask.data.remote.dto.OdooErrorData
import com.example.odootask.data.remote.dto.OdooResponse
import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * Helpers for the repository arg-building tests: build the JSON-RPC envelopes the fake
 * [com.example.odootask.data.remote.OdooApiService] returns. [jsonResult] wraps a raw Odoo
 * result payload; [rpcError] models the `error` branch the repositories surface as failures.
 */

fun jsonResult(rawJson: String): OdooResponse<JsonElement> =
    OdooResponse(result = JsonParser.parseString(rawJson), error = null)

fun rpcError(message: String): OdooResponse<JsonElement> =
    OdooResponse(
        result = null,
        error = OdooError(message = message, data = OdooErrorData(name = "odoo.exceptions", message = message)),
    )
