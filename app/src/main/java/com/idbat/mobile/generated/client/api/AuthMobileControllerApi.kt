package com.idbat.mobile.generated.client.api

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import com.idbat.mobile.generated.client.model.LoginMobileRequest

interface AuthMobileControllerApi {
    /**
     * POST api/auth-mobile/login
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param loginMobileRequest 
     * @return [kotlin.collections.Map<kotlin.String, kotlin.String>]
     */
    @POST("api/auth-mobile/login")
    suspend fun authenticateUser(@Body loginMobileRequest: LoginMobileRequest): Response<kotlin.collections.Map<kotlin.String, kotlin.String>>

    /**
     * GET api/auth-mobile
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.String]
     */
    @GET("api/auth-mobile")
    suspend fun test(): Response<kotlin.String>

}
