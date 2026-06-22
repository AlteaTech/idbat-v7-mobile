package com.idbat.mobile.ui.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.CarteCreeeEntity
import com.idbat.mobile.data.model.CarteCreationQr
import com.idbat.mobile.data.nfc.NfcRepository
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EcriturePuceViewModel @Inject constructor(
    private val nfcRepository: NfcRepository,
    private val database: AppDatabase,
    private val authManager: AuthManager
) : ViewModel() {

    sealed class WriteState {
        object Idle : WriteState()
        object Writing : WriteState()
        data class Success(val uid: String) : WriteState()
        data class Error(val message: String) : WriteState()
    }

    private val _state = MutableStateFlow<WriteState>(WriteState.Idle)
    val state: StateFlow<WriteState> = _state

    fun write(tag: Tag, carteQr: CarteCreationQr, contratId: Long, siteId: Long) {
        // Ignore les taps suivants pendant qu'une écriture est en cours ou déjà réussie
        if (_state.value is WriteState.Writing || _state.value is WriteState.Success) return

        // L'utilisateur connecté (TP) doit être présent pour tracer la création
        val userTpId = authManager.authState.value.loggedInUtilisateurTp?.id
        if (userTpId == null) {
            _state.value = WriteState.Error("Utilisateur non connecté")
            return
        }

        _state.value = WriteState.Writing

        val uid = tag.id.joinToString(" ") { "%02X".format(it) }
        val numeroSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }
        val carte = carteQr.toCartePuce(uid, numeroSerie)

        viewModelScope.launch {
            try {
                nfcRepository.writeCartePuce(tag, carte)

                // Relecture de contrôle
                val relu = nfcRepository.readCartePuce(tag)
                val conforme = relu.numeroIdentification == carte.numeroIdentification &&
                        relu.identClient == carte.identClient &&
                        relu.nomPrenom == carte.nomPrenom &&
                        relu.isCrcValid

                if (!conforme) {
                    _state.value = WriteState.Error("Relecture non conforme après écriture")
                    return@launch
                }

                // Journalise la création réussie en BDD
                database.carteCreeeDao().insert(
                    CarteCreeeEntity(
                        contratId = contratId,
                        siteId = siteId,
                        userTpId = userTpId,
                        type = "P",
                        succes = true,
                        uid = uid,
                        numeroSerie = numeroSerie,
                        numeroIdentification = carteQr.numeroIdentification,
                        motDePasse = carteQr.motDePasse,
                        societe = carteQr.societe,
                        interne = carteQr.interne == "!",
                        prepaiement = carteQr.prepaiement,
                        facturation = carteQr.facturation,
                        gratuit = carteQr.gratuit,
                        nomPrenom = carteQr.nomPrenom,
                        identClient = carteQr.identClient,
                        soldePoints = carteQr.soldePoints,
                        cumulPoints = carteQr.cumulPoints,
                        dateCreation = System.currentTimeMillis()
                    )
                )

                _state.value = WriteState.Success(uid)
            } catch (e: Exception) {
                _state.value = WriteState.Error(e.message ?: "Erreur d'écriture")
            }
        }
    }

    fun reset() {
        _state.value = WriteState.Idle
    }
}
