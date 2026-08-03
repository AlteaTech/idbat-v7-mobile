package com.idbat.mobile.singleton

object ConfigSingleton {
    // ⚠️ Valeurs de REPLI uniquement. Les valeurs effectives sont lues en BDD dans la table
    // `parametre` et rafraîchies à chaque synchro descendante (cf. ParametreManager).

    // Intervalle d'auto-synchro (minutes) — clef TP_TRANSFERT_GPRS_MINUTES
    val defaultSyncIntervalMinutes = 10L

    // Délai d'inactivité avant reconnexion (minutes) — clef FRONT_DELAI_INACTIVITE_MINUTES
    val defaultSessionTimeoutMinutes = 60L

    // RG3 : durée de rétention locale des données saisies (en jours). Les données envoyées
    // avec succès ne sont purgées qu'après ce délai, à chaque synchro réussie.
    // Clef TP_ARCHIVES_JOURS
    val defaultDataRetentionDays = 4L

    const val BASE_URL_DEV_EMULATOR = "http://10.0.2.2:8091/"
    const val BASE_URL_DEV_DEVICE   = "http://localhost:8091/"   // nécessite : adb reverse tcp:8091 tcp:8091
    const val BASE_URL_STAGING      = "https://idbat-mobile-rec.recyclage.veolia.fr/"

    val baseUrl = BASE_URL_DEV_DEVICE

    // Feature flags : affichage des boutons de dev dans CarteActionSheet
    val pocEnable = false          // bouton "POC"
    val testChargeEnable = false   // bouton "Test Volume Passage"
}
