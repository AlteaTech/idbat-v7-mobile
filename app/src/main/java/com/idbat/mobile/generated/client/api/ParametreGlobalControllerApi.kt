package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.ParametreGlobalDmo
import retrofit2.Response
import retrofit2.http.GET

interface ParametreGlobalControllerApi {
    /**
     * GET api/parametres-globaux
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.List<ParametreGlobalDmo>]
     */
    @GET("api/parametres-globaux")
    suspend fun getAllParametres(): Response<kotlin.collections.List<ParametreGlobalDmo>>

}
