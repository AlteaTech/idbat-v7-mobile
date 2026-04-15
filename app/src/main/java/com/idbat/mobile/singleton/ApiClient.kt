package com.idbat.mobile.singleton

import com.idbat.mobile.generated.client.api.AuthMobileControllerApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {

    // 1. On configure Moshi
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // 2. On configure Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(ConfigSingleton.baseUrl) // N'oubliez pas d'adapter l'IP si besoin
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // 3. On expose vos différentes API publiques
    // Le "by lazy" permet de ne créer l'API que la toute première fois qu'on s'en sert
    val authApi: AuthMobileControllerApi by lazy {
        retrofit.create(AuthMobileControllerApi::class.java)
    }

    // Si plus tard vous générez une UserApi ou InvoiceApi,
    // vous rajouterez simplement les lignes ici :
    // val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }
}