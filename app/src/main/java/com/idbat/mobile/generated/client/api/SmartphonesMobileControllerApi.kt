package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.CreerSmartphoneMobileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
    suspend fun checkSmartphoneExists(@Query("numSerie") numSerie: String): Response<Boolean>

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
    suspend fun creerSmartphone(@Body creerSmartphoneMobileRequest: CreerSmartphoneMobileRequest): Response<Any>

}
