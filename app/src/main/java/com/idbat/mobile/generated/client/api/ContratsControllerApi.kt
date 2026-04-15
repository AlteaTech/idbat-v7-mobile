package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.ContratDmo
import retrofit2.Response
import retrofit2.http.GET

interface ContratsControllerApi {
    /**
     * GET api/contrats
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [ContratDmo]
     */
    @GET("api/contrats")
    suspend fun getByDevice(): Response<ContratDmo>

}
