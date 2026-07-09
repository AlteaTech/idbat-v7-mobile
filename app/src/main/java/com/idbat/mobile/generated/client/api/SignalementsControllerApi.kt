package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.CreerSignalementRequest
import com.idbat.mobile.generated.client.model.SignalementDmo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
