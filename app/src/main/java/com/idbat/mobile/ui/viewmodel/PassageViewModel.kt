package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.PassageEntity
import com.idbat.mobile.data.entities.PassageRefuseEntity
import com.idbat.mobile.data.entities.SeuilEtatEntity
import com.idbat.mobile.data.model.PassageSaveState
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PassageViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager
) : ViewModel() {

    private val _saveState = MutableStateFlow<PassageSaveState>(PassageSaveState.Idle)
    val saveState: StateFlow<PassageSaveState> = _saveState.asStateFlow()

    private val _seuils = MutableStateFlow<List<SeuilEtatEntity>>(emptyList())
    val seuils: StateFlow<List<SeuilEtatEntity>> = _seuils.asStateFlow()

    // Liste noire de la carte présentée (null = pas en liste noire / carte inconnue)
    data class ListeNoireInfo(val libelle: String?)
    private val _listeNoire = MutableStateFlow<ListeNoireInfo?>(null)
    val listeNoire: StateFlow<ListeNoireInfo?> = _listeNoire.asStateFlow()

    fun loadSeuils(usagerId: Long?) {
        if (usagerId == null) {
            _seuils.value = emptyList()
            return
        }
        viewModelScope.launch {
            _seuils.value = database.seuilEtatDao().getSeuilsByUsager(usagerId)
        }
    }

    fun loadListeNoire(carteId: Long?) {
        if (carteId == null) {
            _listeNoire.value = null
            return
        }
        viewModelScope.launch {
            val carte = database.carteContratDao().getCarteById(carteId)
            _listeNoire.value = if (carte?.isEnListeNoire == true) {
                ListeNoireInfo(libelle = carte.motifListeNoireLibelle)
            } else {
                null
            }
        }
    }

    fun enregistrerPassage(
        contratId: Long,
        siteId: Long,
        carteId: Long?,
        uidCarte: String? = null,
        soldePointsAvant: Double? = null
    ) {
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
                        numeroBonPassage = numeroBonPassage,
                        uidCarte = uidCarte,
                        soldePointsAvant = soldePointsAvant,
                        // Accès simple : aucune matière → valeur 0, solde inchangé
                        valeurPoints = 0.0,
                        nouveauSoldePoints = soldePointsAvant,
                        // RG1.1 : passage accès simple → mode de paiement 0 (Accès simple)
                        modePaiement = 0,
                        transactionId = java.util.UUID.randomUUID().toString()
                    )
                )
                _saveState.value = PassageSaveState.Success
            } catch (e: Exception) {
                _saveState.value = PassageSaveState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * RG1/RG2/RG3 : enregistre un **passage refusé** (outbox). Appelé au clic sur « Fermer » quand
     * l'usager est bloqué (liste noire / seuil). `commentaire` = message d'alerte justifiant le refus.
     * Best-effort en arrière-plan (pas d'état exposé à l'UI, qui navigue immédiatement).
     */
    fun enregistrerPassageRefuse(
        contratId: Long,
        siteId: Long,
        carteId: Long?,
        commentaire: String,
        usagerId: Long? = null,
        uidCarte: String? = null,
        emailUsager: String? = null
    ) {
        viewModelScope.launch {
            try {
                val userTp = authManager.authState.value.loggedInUtilisateurTp ?: return@launch
                database.passageRefuseDao().insert(
                    PassageRefuseEntity(
                        dateHeure = System.currentTimeMillis(),
                        contratId = contratId,
                        siteId = siteId,
                        carteId = carteId,
                        usagerId = usagerId,
                        userTpId = userTp.id,
                        commentaire = commentaire,
                        uidCarte = uidCarte,
                        emailUsager = emailUsager,
                        transactionId = java.util.UUID.randomUUID().toString()
                    )
                )
            } catch (_: Exception) {
                // best-effort : un échec d'enregistrement ne doit pas bloquer la fermeture de l'écran
            }
        }
    }

    fun resetState() {
        _saveState.value = PassageSaveState.Idle
    }
}
