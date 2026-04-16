package com.idbat.mobile.singleton

import com.idbat.mobile.generated.client.api.AuthMobileControllerApi
import com.idbat.mobile.generated.client.api.ContratsControllerApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {

    // 1. On configure Moshi
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // 2. Intercepteur pour ajouter automatiquement le token Bearer
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = ConfigSingleton.tokenApi
        
        val authenticatedRequest = if (token.isNotEmpty()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        
        chain.proceed(authenticatedRequest)
    }

    // 3. Intercepteur de logging (optionnel, pour debug)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 4. Configuration OkHttp avec les intercepteurs
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // 5. Configuration Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(ConfigSingleton.baseUrl)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // 6. APIs disponibles
    val authApi: AuthMobileControllerApi by lazy {
        retrofit.create(AuthMobileControllerApi::class.java)
    }

    val contratsApi: ContratsControllerApi by lazy {
        retrofit.create(ContratsControllerApi::class.java)
    }
}