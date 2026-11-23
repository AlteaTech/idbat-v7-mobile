package com.idbat.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.CarteInconnueEntity
import com.idbat.mobile.data.entities.MotifAutorisationContratEntity
import com.idbat.mobile.data.entities.PassageInconnuEntity
import com.idbat.mobile.data.entities.PassageInconnuRefuseEntity
import com.idbat.mobile.data.model.PassageInconnuInfo
import com.idbat.mobile.data.model.PassageSaveState
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * #533 : passage d'une immatriculation inconnue (contrôle d'accès seul, sans matières).
 */
@HiltViewModel
class PassageInconnuViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager
) : ViewModel() {

    private val _contratId = MutableStateFlow<Long?>(null)

    // RG2.1 : motifs d'autorisation exceptionnelle du contrat
    @OptIn(ExperimentalCoroutinesApi::class)
    val motifs: StateFlow<List<MotifAutorisationContratEntity>> = _contratId
        .filterNotNull()
        .flatMapLatest { database.motifAutorisationContratDao().getByContratFlow(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _saveState = MutableStateFlow<PassageSaveState>(PassageSaveState.Idle)
    val saveState: StateFlow<PassageSaveState> = _saveState.asStateFlow()

    fun setContratId(id: Long) {
        _contratId.value = id
    }

    /**
     * RG3/RG4 (1er passage) et RG6/RG7 (suivants) : enregistre le passage inconnu (outbox) et met
     * à jour le compteur local de l'immatriculation, dans une même transaction.
     * `motifId`/`motifLibelle` = motif choisi au 1er passage, ou celui déjà mémorisé ensuite.
     */
    fun enregistrerPassage(
        contratId: Long,
        siteId: Long,
        info: PassageInconnuInfo,
        motifId: Long,
        motifLibelle: String?
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
                val immatriculation = info.immatriculation.uppercase()

                database.withTransaction {
                    database.passageInconnuDao().insert(
                        PassageInconnuEntity(
                            dateHeure = dateHeure,
                            contratId = contratId,
                            siteId = siteId,
                            userTpId = userTp.id,
                            immatriculation = immatriculation,
                            motifAutorisationContratId = motifId,
                            numeroBonPassage = "${contrat.trigramme}${site.trigramme}$dateSuffix",
                            transactionId = UUID.randomUUID().toString()
                        )
                    )
                    // RG7 : +1 si l'immatriculation est déjà connue ; RG4 : sinon création à 1
                    val majs = database.carteInconnueDao().incrementerPassages(immatriculation)
                    if (majs == 0) {
                        database.carteInconnueDao().insert(
                            CarteInconnueEntity(
                                valeur = immatriculation,
                                motifAutorisationContratId = motifId,
                                motifAutorisationLibelle = motifLibelle,
                                nbPassages = 1
                            )
                        )
                    }
                }
                _saveState.value = PassageSaveState.Success
            } catch (e: Exception) {
                _saveState.value = PassageSaveState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * RG8.3 : enregistre le refus (max atteint). Best-effort, appelé une seule fois dès l'affichage
     * du refus (comme les passages refusés), pour survivre à un kill de l'app avant « Fermer ».
     */
    fun enregistrerRefus(contratId: Long, siteId: Long, info: PassageInconnuInfo, commentaire: String) {
        val motifId = info.motifAutorisationContratId ?: return
        viewModelScope.launch {
            try {
                val userTp = authManager.authState.value.loggedInUtilisateurTp ?: return@launch
                database.passageInconnuRefuseDao().insert(
                    PassageInconnuRefuseEntity(
                        dateHeure = System.currentTimeMillis(),
                        contratId = contratId,
                        siteId = siteId,
                        userTpId = userTp.id,
                        immatriculation = info.immatriculation.uppercase(),
                        motifAutorisationContratId = motifId,
                        commentaire = commentaire,
                        transactionId = UUID.randomUUID().toString()
                    )
                )
            } catch (_: Exception) {
                // best-effort : ne doit pas bloquer la fermeture de l'écran
            }
        }
    }

    fun resetState() {
        _saveState.value = PassageSaveState.Idle
    }
}
