package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.MarquerCarteQrCodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CarteCreationControllerApi {
    /**
     * POST api/carte-creation
     *
     *
     * Responses:
     *  - 200: OK
     *
     * @param marquerCarteQrCodeRequest
     * @return [Unit]
     */
    @POST("api/carte-creation")
    suspend fun marquerCarteCreationParQrCode(@Body marquerCarteQrCodeRequest: MarquerCarteQrCodeRequest): Response<Unit>

}
