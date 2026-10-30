package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerRechargeCarteRequest

interface RechargesCarteControllerApi {
    /**
     * POST api/recharges-carte
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerRechargeCarteRequest 
     * @return [Unit]
     */
    @POST("api/recharges-carte")
    suspend fun syncRechargesCarte(@Body creerRechargeCarteRequest: CreerRechargeCarteRequest): Response<Unit>

}
