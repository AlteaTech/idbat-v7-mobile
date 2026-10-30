package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerPassageRefuseRequest

interface PassagesRefusesControllerApi {
    /**
     * POST api/passages-refuses
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerPassageRefuseRequest 
     * @return [kotlin.Long]
     */
    @POST("api/passages-refuses")
    suspend fun creerPassageRefuse(@Body creerPassageRefuseRequest: CreerPassageRefuseRequest): Response<kotlin.Long>

}
