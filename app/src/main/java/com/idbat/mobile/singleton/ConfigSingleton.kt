package com.idbat.mobile.singleton

import com.idbat.mobile.generated.client.api.AuthMobileControllerApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ConfigSingleton {
    lateinit var tokenApi: String
    val webEnable = true
    val baseUrl = "http://10.0.2.2:8091/"
}