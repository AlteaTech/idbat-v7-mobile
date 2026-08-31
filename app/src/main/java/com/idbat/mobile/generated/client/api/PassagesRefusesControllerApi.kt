package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.CreerPassageRefuseRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
