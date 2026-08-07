package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.CreerSmartphoneMobileRequest
import com.idbat.mobile.generated.client.model.UpdateSmartphoneLocationRequest
import retrofit2.Response
import retrofit2.http.*

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

    /**
     * PUT api/smartphones/location
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param updateSmartphoneLocationRequest 
     * @return [Unit]
     */
    @PUT("api/smartphones/location")
    suspend fun updateSmartphoneLocation(@Body updateSmartphoneLocationRequest: UpdateSmartphoneLocationRequest): Response<Unit>

}
