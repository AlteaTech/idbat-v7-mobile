package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerPassageRequest

interface PassagesControllerApi {
    /**
     * POST api/passages
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerPassageRequest 
     * @return [kotlin.Long]
     */
    @POST("api/passages")
    suspend fun creer(@Body creerPassageRequest: CreerPassageRequest): Response<kotlin.Long>

}
