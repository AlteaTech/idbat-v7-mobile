package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.CreerSignalementRequest
import com.idbat.mobile.generated.client.model.SignalementDmo

interface SignalementsControllerApi {
    /**
     * POST api/signalements
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param creerSignalementRequest 
     * @return [SignalementDmo]
     */
    @POST("api/signalements")
    suspend fun creerSignalement(@Body creerSignalementRequest: CreerSignalementRequest): Response<SignalementDmo>

}
