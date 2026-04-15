package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.repository.SiteRepository
import com.idbat.mobile.data.repository.UtilisateurRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val availableSites: List<SiteEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val utilisateurRepository: UtilisateurRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadSites()
    }

    private fun loadSites() {
        siteRepository.getAllSites()
            .onEach { sites ->
                _uiState.update { it.copy(availableSites = sites) }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = "Erreur chargement sites: ${e.message}") }
            }
            .launchIn(viewModelScope)
    }

    fun login(username: String, pin: String, siteId: Long?) {
        if (siteId == null) {
            _uiState.update { it.copy(errorMessage = "Veuillez sélectionner un site") }
            return
        }

        if (username.isBlank() || pin.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez remplir tous les champs") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val user = utilisateurRepository.checkCredentials(username, pin)

            if (user != null) {
                utilisateurRepository.updateLastLogin(user)
                _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Identifiant ou mot de passe incorrect"
                    ) 
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { it.copy(isLoginSuccessful = false, errorMessage = null) }
    }
}
