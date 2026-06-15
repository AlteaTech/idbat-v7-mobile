package com.idbat.mobile.singleton

import android.os.Build

object ConfigSingleton {
    val webEnable = true
    
    val IsSyncDescEnable = true
    val IsSyncAscEnable = true

    const val BASE_URL_DEV_EMULATOR = "http://10.0.2.2:8091/"
    const val BASE_URL_DEV_DEVICE   = "http://localhost:8091/"   // nécessite : adb reverse tcp:8091 tcp:8091
    const val BASE_URL_STAGING      = "https://idbat-mobile-rec.recyclage.veolia.fr/"

    val baseUrl = BASE_URL_STAGING
}
