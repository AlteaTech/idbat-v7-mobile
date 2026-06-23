package com.idbat.mobile.singleton

object ConfigSingleton {
    val webEnable = true
    
    val IsSyncDescEnable = true
    val IsSyncAscEnable = true

    // Intervalle de déclenchement de la synchronisation automatique (en minutes)
    val syncIntervalMinutes = 60L

    // RG3 : durée de rétention locale des données saisies (en jours). Les données envoyées
    // avec succès ne sont purgées qu'après ce délai, à chaque synchro réussie.
    val dataRetentionDays = 2L

    // Délai d'inactivité (en minutes) avant reconnexion obligatoire. Si l'app reste en
    // arrière-plan/veille plus longtemps que ce délai, le retour au premier plan force le
    // passage par l'écran de login (la navigation précédente n'est pas restaurée).
    val sessionTimeoutMinutes = 10L

    const val BASE_URL_DEV_EMULATOR = "http://10.0.2.2:8091/"
    const val BASE_URL_DEV_DEVICE   = "http://localhost:8091/"   // nécessite : adb reverse tcp:8091 tcp:8091
    const val BASE_URL_STAGING      = "https://idbat-mobile-rec.recyclage.veolia.fr/"

    val baseUrl = BASE_URL_DEV_DEVICE
}
