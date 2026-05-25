package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.UsagerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AutresCartesViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _contratId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val usagers: StateFlow<List<UsagerEntity>> = _contratId
        .filterNotNull()
        .flatMapLatest { contratId ->
            database.usagerDao().getUsagersByContratFlow(contratId)
        }
        .combine(_searchQuery) { list, query ->
            val filtered = if (query.isBlank()) list
            else list.filter { u ->
                u.nom.contains(query, ignoreCase = true) ||
                u.prenom.contains(query, ignoreCase = true)
            }
            filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, UsagerEntity::nom)
                .thenBy(String.CASE_INSENSITIVE_ORDER, UsagerEntity::prenom))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setContratId(id: Long) {
        _contratId.value = id
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}
