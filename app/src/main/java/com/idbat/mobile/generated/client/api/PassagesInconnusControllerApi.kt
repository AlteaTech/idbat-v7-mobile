package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerPassageInconnuRequest

interface PassagesInconnusControllerApi {
    /**
     * POST api/passages-inconnus
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerPassageInconnuRequest 
     * @return [kotlin.Long]
     */
    @POST("api/passages-inconnus")
    suspend fun creerPassageInconnu(@Body creerPassageInconnuRequest: CreerPassageInconnuRequest): Response<kotlin.Long>

}
