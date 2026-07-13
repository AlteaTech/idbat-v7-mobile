package com.idbat.mobile.ui.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.RechargeCarteEntity
import com.idbat.mobile.data.model.CartePuce
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
        // RG2c : carte non éligible au pré-paiement
        object NotEligible : ReadState()
        // RG2a : carte lue + éligible → formulaire de rechargement (on conserve la CartePuce lue
        // et l'usager associé, pour les journaliser au moment de l'écriture)
        data class Ready(
            val info: RechargeCarteInfo,
            val carteLue: CartePuce,
            val usagerId: Long? = null
        ) : ReadState()
    }

    sealed class WriteState {
        object Idle : WriteState()
        object Writing : WriteState()
        object WrongCard : WriteState()       // RG3.1 : carte présentée ≠ carte lue
        data class Error(val message: String) : WriteState()
        object Success : WriteState()
    }

    private val _state = MutableStateFlow<ReadState>(ReadState.Idle)
    val state: StateFlow<ReadState> = _state

    private val _writeState = MutableStateFlow<WriteState>(WriteState.Idle)
    val writeState: StateFlow<WriteState> = _writeState

    fun read(tag: Tag, contratId: Long) {
        if (_state.value is ReadState.Reading ||
            _state.value is ReadState.Ready ||
            _state.value is ReadState.NotEligible
        ) return
        _state.value = ReadState.Reading
        viewModelScope.launch {
            try {
                val carte = nfcRepository.readCartePuce(tag)
                val dbCarte = database.carteContratDao().getCarteByUidRfid(carte.numeroSerie)
                val usager = dbCarte?.let { database.usagerDao().getUsagerByCarte(it.id) }
                val isPro = usager?.typeApporteurIsPro == true

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
                    info = RechargeCarteInfo(
                        societe = if (isPro) {
                            (usager?.raisonSociale?.takeIf { it.isNotBlank() }
                                ?: carte.societe.takeIf { it.isNotBlank() })
                        } else null,
                        nomTitulaire = usager?.let { "${it.nom} ${it.prenom}" }
                            ?: carte.nomPrenom.takeIf { it.isNotBlank() },
                        numeroCarte = carte.numeroIdentification,
                        typeApporteur = usager?.typeApporteurLibelle,
                        contact = usager?.couriel,
                        soldePoints = carte.soldePoints,
                        typeApporteurIsPro = isPro
                    ),
                    carteLue = carte,
                    usagerId = usager?.id
                )
            } catch (e: Exception) {
                _state.value = ReadState.Error(e.message ?: "Erreur de lecture")
            }
        }
    }

    /**
     * RG3 : réécrit la carte avec le nouveau solde. La carte présentée doit être la même que
     * celle lue (RG3.1). Le rechargement n'est journalisé en base qu'après écriture réussie (RG3.2).
     */
    fun ecrireRechargement(tag: Tag, points: Double, contratId: Long, siteId: Long) {
        if (_writeState.value is WriteState.Writing || _writeState.value is WriteState.Success) return
        val ready = _state.value as? ReadState.Ready ?: return
        val carteLue = ready.carteLue
        _writeState.value = WriteState.Writing

        viewModelScope.launch {
            try {
                // RG3.1 : même carte (même UID/numéro de série)
                val tagSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }
                if (tagSerie != carteLue.numeroSerie) {
                    _writeState.value = WriteState.WrongCard
                    return@launch
                }

                val ancienSolde = carteLue.soldePoints
                val nouveauSolde = ancienSolde + points
                val carteMaj = carteLue.copy(soldePoints = carteLue.soldePoints + points)

                // Écriture NFC (le CRC est recalculé dans serialize())
                nfcRepository.writeCartePuce(tag, carteMaj)

                // RG3.2 : journalisation seulement après écriture réussie
                database.rechargeCarteDao().insert(
                    RechargeCarteEntity(
                        contratId = contratId,
                        siteId = siteId,
                        uid = carteLue.uid,
                        numeroIdentification = carteLue.numeroIdentification,
                        identClient = carteLue.identClient,
                        usagerId = ready.usagerId,
                        ancienSolde = ancienSolde,
                        pointsRecharges = points,
                        nouveauSolde = nouveauSolde,
                        dateRecharge = System.currentTimeMillis()
                    )
                )

                _writeState.value = WriteState.Success
            } catch (e: Exception) {
                _writeState.value = WriteState.Error(e.message ?: "Erreur d'écriture")
            }
        }
    }

    fun resetWrite() {
        _writeState.value = WriteState.Idle
    }

    fun reset() {
        _state.value = ReadState.Idle
        _writeState.value = WriteState.Idle
    }
}
