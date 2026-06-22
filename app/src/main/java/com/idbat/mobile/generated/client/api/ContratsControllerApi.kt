package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.ContratDmo

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
