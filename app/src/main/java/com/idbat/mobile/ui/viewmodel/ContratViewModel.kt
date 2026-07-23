package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.ContratEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Charge un contrat par son id depuis la BDD et l'expose en flux live. Toute modification en
 * base (ex. synchro descendante mettant à jour les flags du contrat) se répercute aussitôt,
 * et le contrat est donc toujours à jour / rechargé quand on (re)vient sur un écran.
 *
 * Les écrans ne reçoivent plus un `ContratEntity` figé en paramètre mais uniquement un
 * `contratId: Long`, et appellent [setContratId] pour observer ce contrat.
 */
@HiltViewModel
class ContratViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _contratId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val contrat: StateFlow<ContratEntity?> = _contratId
        .flatMapLatest { id ->
            if (id == null || id == 0L) flowOf(null)
            else database.contratDao().getContratByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setContratId(id: Long) {
        _contratId.value = id
    }
}
