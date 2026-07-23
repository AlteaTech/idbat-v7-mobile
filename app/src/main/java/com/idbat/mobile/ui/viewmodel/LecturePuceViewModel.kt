package com.idbat.mobile.ui.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.nfc.NfcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lecture d'une carte à puce pour un passage/dépôt : lit la carte, retrouve l'usager
 * associé (carte active de type 'P') et construit l'InfoCartePassage. Si aucune carte
 * active n'est trouvée → NotRecognized (« Carte non reconnue »).
 */
@HiltViewModel
class LecturePuceViewModel @Inject constructor(
    private val nfcRepository: NfcRepository,
    private val database: AppDatabase
) : ViewModel() {

    sealed class ReadState {
        object Idle : ReadState()
        object Reading : ReadState()
        data class Error(val message: String) : ReadState()
        object NotRecognized : ReadState()
        data class Ready(val info: InfoCartePassage) : ReadState()
    }

    private val _state = MutableStateFlow<ReadState>(ReadState.Idle)
    val state: StateFlow<ReadState> = _state

    fun read(tag: Tag) {
        if (_state.value is ReadState.Reading ||
            _state.value is ReadState.Ready ||
            _state.value is ReadState.NotRecognized
        ) return
        _state.value = ReadState.Reading
        viewModelScope.launch {
            try {
                val carte = nfcRepository.readCartePuce(tag)
                val now = System.currentTimeMillis()
                val dbCarte = database.carteContratDao().getActiveCartePByUid(carte.numeroSerie, now)
                if (dbCarte == null) {
                    _state.value = ReadState.NotRecognized
                    return@launch
                }
                val usager = database.usagerDao().getUsagerByCarte(dbCarte.id)
                _state.value = ReadState.Ready(
                    InfoCartePassage(
                        societe = usager?.raisonSociale?.takeIf { it.isNotBlank() },
                        nomTitulaire = usager?.let { "${it.nom} ${it.prenom}" }
                            ?: carte.nomPrenom.takeIf { it.isNotBlank() },
                        numeroCarte = carte.numeroIdentification,
                        typeApporteur = usager?.typeApporteurLibelle,
                        contact = usager?.couriel,
                        carteId = dbCarte.id,
                        typeApporteurIsPro = usager?.typeApporteurIsPro,
                        usagerId = usager?.id,
                        uid = carte.uid,
                        soldePoints = carte.soldePoints
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
