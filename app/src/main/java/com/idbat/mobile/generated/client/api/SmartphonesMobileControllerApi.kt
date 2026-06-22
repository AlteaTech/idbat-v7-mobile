package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerSmartphoneMobileRequest

interface SmartphonesMobileControllerApi {
    /**
     * GET api/smartphones/check
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param numSerie 
     * @return [kotlin.Boolean]
     */
    @GET("api/smartphones/check")
    suspend fun checkSmartphoneExists(@Query("numSerie") numSerie: kotlin.String): Response<kotlin.Boolean>

    /**
     * POST api/smartphones
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerSmartphoneMobileRequest 
     * @return [kotlin.Any]
     */
    @POST("api/smartphones")
    suspend fun creerSmartphone(@Body creerSmartphoneMobileRequest: CreerSmartphoneMobileRequest): Response<kotlin.Any>

}
