package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.CreerRechargeCarteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
