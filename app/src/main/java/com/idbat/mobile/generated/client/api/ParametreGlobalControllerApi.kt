package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.ParametreGlobalDmo

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
