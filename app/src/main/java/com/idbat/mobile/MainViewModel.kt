package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.singleton.AuthManager
import com.idbat.mobile.singleton.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val syncManager: SyncManager
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
        viewModelScope.launch {
            _uiState.value.authState.loggedInSite?.let { site ->
                syncManager.executeTransfer(site)
            }
        }
    }
}
