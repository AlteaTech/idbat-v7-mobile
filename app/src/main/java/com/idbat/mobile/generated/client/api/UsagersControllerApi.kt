package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.PatchUsagerMobileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

interface UsagersControllerApi {
    /**
     * PUT api/usagers
     *
     *
     * Responses:
     *  - 200: OK
     *
     * @param patchUsagerMobileRequest
     * @return [Unit]
     */
    @PUT("api/usagers")
    suspend fun patchCourrielsBulk(@Body patchUsagerMobileRequest: List<PatchUsagerMobileRequest>): Response<Unit>

}
