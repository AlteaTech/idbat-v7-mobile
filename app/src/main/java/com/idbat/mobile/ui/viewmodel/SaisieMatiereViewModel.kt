package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.MatiereSiteEntity
import com.idbat.mobile.data.model.SaisieMatiereLigne
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SaisieMatiereViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _siteId = MutableStateFlow(0L)

    val matieres: StateFlow<List<MatiereSiteEntity>> = _siteId
        .flatMapLatest { id -> database.matiereSiteDao().getMatieresBySiteFlow(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lignes = MutableStateFlow<List<SaisieMatiereLigne>>(emptyList())
    val lignes: StateFlow<List<SaisieMatiereLigne>> = _lignes.asStateFlow()

    fun setSiteId(id: Long) {
        _siteId.value = id
    }

    fun resetLignes() {
        _lignes.value = emptyList()
    }

    fun ajouterLigne(ligne: SaisieMatiereLigne) {
        _lignes.value = _lignes.value + ligne
    }

    fun modifierLigne(index: Int, ligne: SaisieMatiereLigne) {
        val list = _lignes.value.toMutableList()
        list[index] = ligne
        _lignes.value = list
    }

    fun supprimerLigne(index: Int) {
        val list = _lignes.value.toMutableList()
        list.removeAt(index)
        _lignes.value = list
    }
}
