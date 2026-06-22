package com.idbat.mobile.ui.viewmodel

import android.content.Context
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.service.SyncService
import com.idbat.mobile.singleton.AuthManager
import com.idbat.mobile.singleton.ConfigSingleton
import com.idbat.mobile.singleton.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        // Auto-synchro périodique : déclenche un transfert toutes les X minutes
        // (intervalle dans ConfigSingleton), uniquement si connecté et qu'aucun
        // transfert n'est déjà en cours. La montante et la descendante restent
        // indépendantes (cf. SyncManager : l'une s'exécute même si l'autre n'a rien à faire).
        viewModelScope.launch {
            while (isActive) {
                delay(ConfigSingleton.syncIntervalMinutes * 60_000L)
                val state = _uiState.value
                if (state.isLoggedIn && !state.syncState.isTransferring) {
                    executeTransfer()
                }
            }
        }
    }

    suspend fun getSuiviContentAsync(siteId: Long): CharSequence {
        val operationsTentees = database.lastSynchroHistoryDao().getTotalOperationsTentees(siteId) ?: 0L
        val operationsReussies = database.lastSynchroHistoryDao().getTotalOperationsReussies(siteId) ?: 0L
        val operationsEchouees = operationsTentees - operationsReussies

        return buildSuiviContent(
            siteId,
            _uiState.value.syncState.lastSynchroDateEnvoi,
            _uiState.value.syncState.lastSynchroDateReception,
            operationsTentees,
            operationsEchouees
        )
    }


    private fun buildSuiviContent(
        siteId: Long,
        lastEnvoi: Date?,
        lastReception: Date?,
        operation: Long,
        operationFailed: Long
    ): CharSequence { // 1. On retourne un CharSequence au lieu d'un String
        val formatter = SimpleDateFormat("dd/MM/yyyy 'à' HH'h'mm", Locale.FRANCE)

        // 2. On utilise buildSpannedString au lieu de buildString
        return buildSpannedString {
            bold { append("Dernier envoi réussi le :") }
            append("\n${lastEnvoi?.let { formatter.format(it) } ?: "Jamais"}\n\n")

            bold { append("Dernière réception réussie le :") }
            append("\n${lastReception?.let { formatter.format(it) } ?: "Jamais"}\n\n")

            bold { append("Opérations :") }
            append("\n$operation\n\n")

            bold { append("Opérations non transférées :") }
            append("\n$operationFailed\n")
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
