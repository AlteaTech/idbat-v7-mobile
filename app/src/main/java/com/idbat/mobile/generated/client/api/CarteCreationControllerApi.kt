package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.MarquerCarteQrCodeRequest

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
