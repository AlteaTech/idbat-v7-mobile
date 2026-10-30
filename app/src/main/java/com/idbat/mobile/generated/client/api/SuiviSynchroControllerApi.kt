package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.SuiviSynchroRequest

interface SuiviSynchroControllerApi {
    /**
     * POST api/suivi-synchro
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param suiviSynchroRequest 
     * @return [Unit]
     */
    @POST("api/suivi-synchro")
    suspend fun creerOuMettreAJourSuiviSynchro(@Body suiviSynchroRequest: SuiviSynchroRequest): Response<Unit>

}
