package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.PassageEntity
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class PassageSaveState {
    object Idle    : PassageSaveState()
    object Saving  : PassageSaveState()
    object Success : PassageSaveState()
    data class Error(val message: String) : PassageSaveState()
}

@HiltViewModel
class PassageViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager
) : ViewModel() {

    private val _saveState = MutableStateFlow<PassageSaveState>(PassageSaveState.Idle)
    val saveState: StateFlow<PassageSaveState> = _saveState.asStateFlow()

    fun enregistrerPassage(contratId: Long, siteId: Long, carteId: Long?) {
        if (_saveState.value == PassageSaveState.Saving) return
        viewModelScope.launch {
            _saveState.value = PassageSaveState.Saving
            try {
                val userTp = authManager.authState.value.loggedInUtilisateurTp
                    ?: throw IllegalStateException("Utilisateur non connecté")

                val contrat = database.contratDao().getContratById(contratId)
                    ?: throw IllegalStateException("Contrat introuvable")

                val site = database.siteDao().getSiteById(siteId)
                    ?: throw IllegalStateException("Site introuvable")

                val dateHeure = System.currentTimeMillis()
                val dateSuffix = SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(Date(dateHeure))
                val numeroBonPassage = "${contrat.trigramme}${site.trigramme}$dateSuffix"

                database.passageDao().insertPassage(
                    PassageEntity(
                        dateHeure = dateHeure,
                        contratId = contratId,
                        siteId = siteId,
                        carteId = carteId,
                        userTpId = userTp.id,
                        numeroBonPassage = numeroBonPassage
                    )
                )
                _saveState.value = PassageSaveState.Success
            } catch (e: Exception) {
                _saveState.value = PassageSaveState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun resetState() {
        _saveState.value = PassageSaveState.Idle
    }
}
