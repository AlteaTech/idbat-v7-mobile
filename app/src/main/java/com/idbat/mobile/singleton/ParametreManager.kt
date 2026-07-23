package com.idbat.mobile.singleton

import android.util.Log
import com.idbat.mobile.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paramètres globaux applicatifs lus dans la table `parametre` (alimentée à chaque synchro
 * descendante par `GET api/parametres-globaux`).
 *
 * Les valeurs sont exposées en [StateFlow] et mises en cache : certains consommateurs
 * (`AuthManager.onAppForegrounded`) sont appelés hors coroutine et doivent lire la valeur
 * de façon synchrone.
 *
 * Rafraîchi au démarrage (`MainViewModel`) puis après chaque synchro descendante (`SyncManager`).
 * Une clef absente, non numérique ou <= 0 retombe sur la valeur de repli de [ConfigSingleton].
 */
@Singleton
class ParametreManager @Inject constructor(
    private val database: AppDatabase
) {

    /** Intervalle d'auto-synchro, en minutes (clef [PARAM_TP_TRANSFERT_GPRS_MINUTES]). */
    private val _syncIntervalMinutes = MutableStateFlow(ConfigSingleton.defaultSyncIntervalMinutes)
    val syncIntervalMinutes: StateFlow<Long> = _syncIntervalMinutes

    /** Délai d'inactivité avant reconnexion, en minutes (clef [PARAM_FRONT_DELAI_INACTIVITE_MINUTES]). */
    private val _sessionTimeoutMinutes = MutableStateFlow(ConfigSingleton.defaultSessionTimeoutMinutes)
    val sessionTimeoutMinutes: StateFlow<Long> = _sessionTimeoutMinutes

    /** RG3 : durée de rétention locale des données envoyées, en jours (clef [PARAM_TP_ARCHIVES_JOURS]). */
    private val _dataRetentionDays = MutableStateFlow(ConfigSingleton.defaultDataRetentionDays)
    val dataRetentionDays: StateFlow<Long> = _dataRetentionDays

    /** Relit tous les paramètres depuis la base et met à jour les flux. */
    suspend fun refreshAll() {
        updateIfChanged(
            flow = _syncIntervalMinutes,
            clef = PARAM_TP_TRANSFERT_GPRS_MINUTES,
            unite = "min",
            nouvelle = readPositiveLong(PARAM_TP_TRANSFERT_GPRS_MINUTES, ConfigSingleton.defaultSyncIntervalMinutes)
        )
        updateIfChanged(
            flow = _sessionTimeoutMinutes,
            clef = PARAM_FRONT_DELAI_INACTIVITE_MINUTES,
            unite = "min",
            nouvelle = readPositiveLong(PARAM_FRONT_DELAI_INACTIVITE_MINUTES, ConfigSingleton.defaultSessionTimeoutMinutes)
        )
        updateIfChanged(
            flow = _dataRetentionDays,
            clef = PARAM_TP_ARCHIVES_JOURS,
            unite = "j",
            nouvelle = readPositiveLong(PARAM_TP_ARCHIVES_JOURS, ConfigSingleton.defaultDataRetentionDays)
        )
    }

    private fun updateIfChanged(flow: MutableStateFlow<Long>, clef: String, unite: String, nouvelle: Long) {
        if (flow.value != nouvelle) {
            Log.d("PARAMETRE_MANAGER", "$clef : ${flow.value} → $nouvelle $unite")
        }
        flow.value = nouvelle
    }

    /** Lit une clef numérique. Absente / non numérique / <= 0 → [defaut]. */
    private suspend fun readPositiveLong(clef: String, defaut: Long): Long {
        val valeur = runCatching { database.parametreDao().getValeur(clef) }
            .onFailure { Log.e("PARAMETRE_MANAGER", "Lecture du paramètre $clef impossible", it) }
            .getOrNull()

        return valeur?.trim()?.toLongOrNull()?.takeIf { it > 0 } ?: defaut
    }

    companion object {
        const val PARAM_TP_TRANSFERT_GPRS_MINUTES = "TP_TRANSFERT_GPRS_MINUTES"
        const val PARAM_FRONT_DELAI_INACTIVITE_MINUTES = "FRONT_DELAI_INACTIVITE_MINUTES"
        const val PARAM_TP_ARCHIVES_JOURS = "TP_ARCHIVES_JOURS"
    }
}
