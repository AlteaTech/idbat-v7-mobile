package com.idbat.mobile.ui.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.model.RechargeCarteInfo
import com.idbat.mobile.data.nfc.NfcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RechargeCarteViewModel @Inject constructor(
    private val nfcRepository: NfcRepository,
    private val database: AppDatabase
) : ViewModel() {

    sealed class ReadState {
        object Idle : ReadState()
        object Reading : ReadState()
        data class Error(val message: String) : ReadState()
        // RG2c : carte non éligible au pré-paiement (selon pro/particulier + flags contrat)
        object NotEligible : ReadState()
        // RG2a : carte lue + éligible → on affiche le formulaire de rechargement
        data class Ready(val info: RechargeCarteInfo) : ReadState()
    }

    private val _state = MutableStateFlow<ReadState>(ReadState.Idle)
    val state: StateFlow<ReadState> = _state

    fun read(tag: Tag, contratId: Long) {
        if (_state.value is ReadState.Reading ||
            _state.value is ReadState.Ready ||
            _state.value is ReadState.NotEligible
        ) return
        _state.value = ReadState.Reading
        viewModelScope.launch {
            try {
                val carte = nfcRepository.readCartePuce(tag)

                // Lien carte physique → carte BDD (par UID) → usager
                val dbCarte = database.carteContratDao().getCarteByUidRfid(carte.numeroSerie)
                val usager = dbCarte?.let { database.usagerDao().getUsagerByCarte(it.id) }
                val isPro = usager?.typeApporteurIsPro == true

                // RG2c : éligibilité au pré-paiement selon le type d'apporteur + flags contrat
                val contrat = database.contratDao().getContratById(contratId)
                val eligible = if (isPro) {
                    contrat?.hasPrepaiementProfessionnels == true
                } else {
                    contrat?.hasPrepaiementParticuliers == true
                }
                if (!eligible) {
                    _state.value = ReadState.NotEligible
                    return@launch
                }

                _state.value = ReadState.Ready(
                    RechargeCarteInfo(
                        // Société affichée uniquement si l'usager est pro (RG2a.1Fix)
                        societe = if (isPro) {
                            (usager?.raisonSociale?.takeIf { it.isNotBlank() }
                                ?: carte.societe.takeIf { it.isNotBlank() })
                        } else null,
                        nomTitulaire = usager?.let { "${it.nom} ${it.prenom}" }
                            ?: carte.nomPrenom.takeIf { it.isNotBlank() },
                        numeroCarte = carte.numeroIdentification,
                        typeApporteur = usager?.typeApporteurLibelle,
                        contact = usager?.couriel,
                        soldePoints = carte.soldePoints.toInt(),
                        typeApporteurIsPro = isPro
                    )
                )
            } catch (e: Exception) {
                _state.value = ReadState.Error(e.message ?: "Erreur de lecture")
            }
        }
    }

    fun reset() {
        _state.value = ReadState.Idle
    }
}
