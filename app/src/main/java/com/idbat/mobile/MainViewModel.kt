package com.idbat.mobile.ui.viewmodel

import android.content.Context
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.service.SyncService
import com.idbat.mobile.singleton.AuthManager
import com.idbat.mobile.singleton.ParametreManager
import com.idbat.mobile.singleton.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val parametreManager: ParametreManager,
    private val database: AppDatabase // Injectez la base de données

) : ViewModel() {

    data class UiState(
        val isInitialized: Boolean = false,
        val isLoggedIn: Boolean = false,
        val authState: AuthManager.AuthState = AuthManager.AuthState(),
        val syncState: SyncManager.SyncState = SyncManager.SyncState()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        // Combine les états des managers
        viewModelScope.launch {
            combine(
                authManager.authState,
                syncManager.syncState
            ) { authState, syncState ->
                UiState(
                    isInitialized = authState.isInitialized,
                    isLoggedIn = authState.isLoggedIn,
                    authState = authState,
                    syncState = syncState
                )
            }.collect { newState ->
                _uiState.value = newState

                // Charger les dates de synchro quand un site est sélectionné
                newState.authState.loggedInSite?.let { site ->
                    syncManager.loadSyncDatesForSite(site)
                }
            }
        }

        // Auto-synchro périodique : déclenche un transfert toutes les X minutes, uniquement si
        // connecté et qu'aucun transfert n'est déjà en cours. L'intervalle vient du paramètre
        // global TP_TRANSFERT_GPRS_MINUTES (table `parametre`). On **observe** le StateFlow via
        // collectLatest : dès qu'une synchro descendante change la valeur, le delay en cours est
        // annulé et la boucle redémarre avec le nouvel intervalle (sinon un delay déjà lancé avec
        // l'ancienne valeur devrait d'abord s'écouler). La montante et la descendante restent
        // indépendantes (cf. SyncManager).
        viewModelScope.launch {
            // Valeur initiale depuis la BDD (avant toute synchro descendante de cette session)
            parametreManager.refreshAll()
            parametreManager.syncIntervalMinutes.collectLatest { minutes ->
                while (isActive) {
                    delay(minutes * 60_000L)
                    val state = _uiState.value
                    if (state.isLoggedIn && !state.syncState.isTransferring) {
                        executeTransfer()
                    }
                }
            }
        }
    }

    suspend fun getSuiviContentAsync(siteId: Long): CharSequence {
        // Suivi des transferts : nombre d'enregistrements locaux des opérations
        // (passages/dépôts + signalements/événements + cartes créées ; les futurs flux —
        // rechargement carte, maj e-mail usager, maj uid RFID — viendront s'ajouter ici).
        // RG3 : "Opérations" = tout ce qui est en base (déjà transféré non purgé + non transféré).
        val operations =
            database.passageDao().count() +
            database.signalementDao().count() +
            database.carteCreeeDao().count() +
            database.rechargeCarteDao().count() +
            database.passageRefuseDao().count()

        // "Opérations non transférées" = uniquement les lignes pas encore montées vers le BO
        // (sentAt IS NULL). En théorie 0 juste après une synchro réussie.
        val operationsNonTransferees =
            database.passageDao().countUnsent() +
            database.signalementDao().countUnsent() +
            database.carteCreeeDao().countUnsent() +
            database.rechargeCarteDao().countUnsent() +
            database.passageRefuseDao().countUnsent()

        return buildSuiviContent(
            _uiState.value.syncState.lastSynchroDateEnvoi,
            _uiState.value.syncState.lastSynchroDateReception,
            operations,
            operationsNonTransferees
        )
    }


    private fun buildSuiviContent(
        lastEnvoi: Date?,
        lastReception: Date?,
        operations: Long,
        operationsNonTransferees: Long
    ): CharSequence { // 1. On retourne un CharSequence au lieu d'un String
        val formatter = SimpleDateFormat("dd/MM/yyyy 'à' HH'h'mm", Locale.FRANCE)

        // 2. On utilise buildSpannedString au lieu de buildString
        return buildSpannedString {
            bold { append("Dernier envoi réussi le :") }
            append("\n${lastEnvoi?.let { formatter.format(it) } ?: "Jamais"}\n\n")

            bold { append("Dernière réception réussie le :") }
            append("\n${lastReception?.let { formatter.format(it) } ?: "Jamais"}\n\n")

            // RG1/RG2 : valeur sur la même ligne que le libellé — RG3 : pas de ligne vide finale
            bold { append("Opérations : ") }
            append("$operations\n\n")

            bold { append("Opérations non transférées : ") }
            append("$operationsNonTransferees")
        }
    }

    fun initializeApp() {
        viewModelScope.launch {
            authManager.initializeApp()
        }
    }

    fun login(username: String, password: String, siteId: Long?) {
        viewModelScope.launch {
            authManager.login(username, password, siteId)
        }
    }

    fun logout() {
        authManager.logout()
        syncManager.clearSyncData()
    }

    fun executeTransfer() {
        // Le transfert tourne dans un foreground service : il survit à l'écran éteint / au doze.
        // L'UI continue d'observer syncManager.syncState (singleton partagé) pour le loader et les dates.
        _uiState.value.authState.loggedInSite?.let { site ->
            SyncService.start(context, site.id)
        }
    }

    fun clearSyncError() {
        syncManager.clearSyncError()
    }
}
