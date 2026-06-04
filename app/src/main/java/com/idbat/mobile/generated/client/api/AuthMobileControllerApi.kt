package com.idbat.mobile.generated.client.api

import com.idbat.mobile.generated.client.model.LoginMobileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
