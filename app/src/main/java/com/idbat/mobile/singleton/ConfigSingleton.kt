package com.idbat.mobile.singleton

object ConfigSingleton {
    val defaultSyncIntervalMinutes = 10L
    val defaultSessionTimeoutMinutes = 60L
    val defaultDataRetentionDays = 4L

    const val BASE_URL_DEV_EMULATOR = "http://10.0.2.2:8091/"
    const val BASE_URL_DEV_DEVICE   = "http://localhost:8091/"   // nécessite : adb reverse tcp:8091 tcp:8091
    const val BASE_URL_STAGING      = "https://idbat-mobile-rec.recyclage.veolia.fr/"

    val baseUrl = BASE_URL_STAGING

    // Feature flags : affichage des boutons de dev dans CarteActionSheet
    val pocEnable = false          // bouton "POC"
    val testChargeEnable = false   // bouton "Test Volume Passage"
}
