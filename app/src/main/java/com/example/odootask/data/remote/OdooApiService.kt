package com.example.odootask.data.remote

import com.example.odootask.data.remote.dto.OdooRequest
import com.example.odootask.data.remote.dto.OdooResponse
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Single Odoo JSON-RPC endpoint. The `params.service` field on the request body
 * selects between authentication (`common`) and data operations (`object`), so one
 * method covers every call. The raw [JsonElement] result is parsed per-call in the
 * repositories, since Odoo returns a uid, an id, a boolean, or a record array.
 */
interface OdooApiService {

    @POST("jsonrpc")
    suspend fun callJsonRpc(@Body body: OdooRequest): OdooResponse<JsonElement>
}
