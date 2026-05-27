package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.CreerPassageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
