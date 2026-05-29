package com.idbat.mobile.singleton

object ConfigSingleton {
    val webEnable = true

    const val BASE_URL_DEV     = "http://10.0.2.2:8091/"
    const val BASE_URL_STAGING = "https://idbat-mobile-rec.recyclage.veolia.fr/"

    val baseUrl = BASE_URL_STAGING
}
