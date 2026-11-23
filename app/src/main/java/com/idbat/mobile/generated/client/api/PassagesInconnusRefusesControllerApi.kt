package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerPassageInconnuRefuseRequest

interface PassagesInconnusRefusesControllerApi {
    /**
     * POST api/passages-inconnus-refuses
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerPassageInconnuRefuseRequest 
     * @return [kotlin.Long]
     */
    @POST("api/passages-inconnus-refuses")
    suspend fun creerPassageInconnuRefuse(@Body creerPassageInconnuRefuseRequest: CreerPassageInconnuRefuseRequest): Response<kotlin.Long>

}
