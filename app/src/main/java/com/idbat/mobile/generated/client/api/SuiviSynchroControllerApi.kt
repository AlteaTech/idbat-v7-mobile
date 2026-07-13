package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.SuiviSynchroRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
