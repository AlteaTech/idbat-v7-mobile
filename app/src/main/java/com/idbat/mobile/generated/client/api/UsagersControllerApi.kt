package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.PatchUsagerMobileRequest

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
    suspend fun patchCourrielsBulk(@Body patchUsagerMobileRequest: kotlin.collections.List<PatchUsagerMobileRequest>): Response<Unit>

}
