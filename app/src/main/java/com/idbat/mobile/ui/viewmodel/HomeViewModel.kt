package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.TypeSynchro
import com.idbat.mobile.data.repository.SiteRepository
import com.idbat.mobile.data.repository.SynchroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class HomeUiState(
    val selectedSite: SiteEntity? = null,
    val lastSynchroEnvoi: Date? = null,
    val lastSynchroReception: Date? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val synchroRepository: SynchroRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Récupérer le siteId passé en paramètre de navigation (si on en a un)
        // Pour l'instant, on suppose qu'il est passé via un argument "siteId"
        // val siteId: Long? = savedStateHandle.get("siteId")
        // if (siteId != null) {
        //     loadSiteData(siteId)
        // }
    }

    // Cette fonction sera appelée depuis l'UI quand le site est connu
    fun loadSiteData(siteId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // On pourrait récupérer le site complet ici si besoin
            // val site = siteRepository.getSiteById(siteId)
            
            val envoi = synchroRepository.getLastSynchroForSite(siteId, TypeSynchro.ENVOI)
            val reception = synchroRepository.getLastSynchroForSite(siteId, TypeSynchro.RECEPTION)

            _uiState.update { 
                it.copy(
                    // selectedSite = site,
                    lastSynchroEnvoi = envoi?.date,
                    lastSynchroReception = reception?.date,
                    isLoading = false
                )
            }
        }
    }

    fun recordTransfer(siteId: Long) {
        viewModelScope.launch {
            synchroRepository.recordSynchro(siteId, TypeSynchro.ENVOI)
            // Recharger les données pour mettre à jour l'UI
            loadSiteData(siteId)
        }
    }
}
