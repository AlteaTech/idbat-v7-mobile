package com.idbat.mobile.di

import com.idbat.mobile.generated.client.api.AuthMobileControllerApi
import com.idbat.mobile.generated.client.api.ContratsControllerApi
import com.idbat.mobile.generated.client.api.PassagesControllerApi
import com.idbat.mobile.generated.client.api.SignalementsControllerApi
import com.idbat.mobile.generated.client.api.SmartphonesMobileControllerApi
import com.idbat.mobile.singleton.ConfigSingleton
import com.idbat.mobile.singleton.TokenStore
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Singleton

private class LocalDateAdapter {
    @FromJson fun fromJson(value: String): LocalDate = LocalDate.parse(value)
    @ToJson fun toJson(value: LocalDate): String = value.toString()
}

private class OffsetDateTimeAdapter {
    @FromJson fun fromJson(value: String): OffsetDateTime =
        try { OffsetDateTime.parse(value) }
        catch (_: Exception) { java.time.LocalDateTime.parse(value).atOffset(java.time.ZoneOffset.UTC) }
    @ToJson fun toJson(value: OffsetDateTime): String = value.toLocalDateTime().toString()
}

private class BigDecimalAdapter {
    @FromJson fun fromJson(value: String): BigDecimal = BigDecimal(value)
    @ToJson fun toJson(value: BigDecimal): String = value.toPlainString()
}

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .add(OffsetDateTimeAdapter())
        .add(BigDecimalAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: TokenStore): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.token
            val request = if (token.isNotEmpty()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(ConfigSingleton.baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthMobileControllerApi =
        retrofit.create(AuthMobileControllerApi::class.java)

    @Provides
    @Singleton
    fun provideContratsApi(retrofit: Retrofit): ContratsControllerApi =
        retrofit.create(ContratsControllerApi::class.java)

    @Provides
    @Singleton
    fun provideSmartphonesApi(retrofit: Retrofit): SmartphonesMobileControllerApi =
        retrofit.create(SmartphonesMobileControllerApi::class.java)

    @Provides
    @Singleton
    fun providePassagesApi(retrofit: Retrofit): PassagesControllerApi =
        retrofit.create(PassagesControllerApi::class.java)

    @Provides
    @Singleton
    fun provideSignalementsApi(retrofit: Retrofit): SignalementsControllerApi =
        retrofit.create(SignalementsControllerApi::class.java)
}
