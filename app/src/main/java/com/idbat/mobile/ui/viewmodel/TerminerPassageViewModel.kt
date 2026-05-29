package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.PassageEntity
import com.idbat.mobile.data.entities.PassageMatiereEntity
import com.idbat.mobile.data.model.SaisieMatiereLigne
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class TerminerSaveState {
    object Idle    : TerminerSaveState()
    object Saving  : TerminerSaveState()
    object Success : TerminerSaveState()
    data class Error(val message: String) : TerminerSaveState()
}

@HiltViewModel
class TerminerPassageViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager
) : ViewModel() {

    private val _saveState = MutableStateFlow<TerminerSaveState>(TerminerSaveState.Idle)
    val saveState: StateFlow<TerminerSaveState> = _saveState.asStateFlow()

    fun terminer(
        contratId: Long,
        siteId: Long,
        carteId: Long?,
        commentaire: String,
        lignes: List<SaisieMatiereLigne>,
        emailSaisi: String?,
        usagerId: Long?
    ) {
        if (_saveState.value == TerminerSaveState.Saving) return
        viewModelScope.launch {
            _saveState.value = TerminerSaveState.Saving
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

                // Enregistrement du passage
                val passageId = database.passageDao().insertPassage(
                    PassageEntity(
                        dateHeure = dateHeure,
                        contratId = contratId,
                        siteId = siteId,
                        carteId = carteId,
                        userTpId = userTp.id,
                        numeroBonPassage = numeroBonPassage,
                        commentaire = commentaire.takeIf { it.isNotBlank() }
                    )
                )

                // Enregistrement des matières
                val passageMatieres = lignes.map { ligne ->
                    PassageMatiereEntity(
                        passageId = passageId,
                        matiereId = ligne.matiere.matiereId,
                        siteId = ligne.matiere.siteId,
                        libelle = ligne.matiere.libelle,
                        quantite = ligne.quantite,
                        tarif = ligne.matiere.tarif,
                        unitesLibelle = ligne.matiere.unitesDesApportLibelle
                    )
                }
                database.passageMatiereDao().insertMatieres(passageMatieres)

                // Mise à jour du courriel si saisi/modifié (RG5.4 — local uniquement, sync à prévoir)
                if (!emailSaisi.isNullOrBlank() && usagerId != null) {
                    database.usagerDao().updateCouriel(usagerId, emailSaisi)
                }

                _saveState.value = TerminerSaveState.Success
            } catch (e: Exception) {
                _saveState.value = TerminerSaveState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun resetState() {
        _saveState.value = TerminerSaveState.Idle
    }
}
