package com.idbat.mobile.ui.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.model.CartePuce
import com.idbat.mobile.data.nfc.NfcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EcriturePuceViewModel @Inject constructor(
    private val nfcRepository: NfcRepository
) : ViewModel() {

    sealed class WriteState {
        object Idle : WriteState()
        object Writing : WriteState()
        data class Success(val uid: String) : WriteState()
        data class Error(val message: String) : WriteState()
    }

    private val _state = MutableStateFlow<WriteState>(WriteState.Idle)
    val state: StateFlow<WriteState> = _state

    fun write(tag: Tag, carte: CartePuce) {
        // Ignore les taps suivants pendant qu'une écriture est déjà en cours,
        // ou si on a déjà réussi (carte encore dans le champ).
        if (_state.value is WriteState.Writing || _state.value is WriteState.Success) return
        _state.value = WriteState.Writing
        viewModelScope.launch {
            try {
                nfcRepository.writeCartePuce(tag, carte)

                // Relecture de contrôle : on s'assure que le contenu écrit est bien relu
                val relu = nfcRepository.readCartePuce(tag)
                val conforme = relu.numeroIdentification == carte.numeroIdentification &&
                        relu.identClient == carte.identClient &&
                        relu.nomPrenom == carte.nomPrenom &&
                        relu.isCrcValid

                if (conforme) {
                    _state.value = WriteState.Success(carte.uid)
                } else {
                    _state.value = WriteState.Error("Relecture non conforme après écriture")
                }
            } catch (e: Exception) {
                _state.value = WriteState.Error(e.message ?: "Erreur d'écriture")
            }
        }
    }

    fun reset() {
        _state.value = WriteState.Idle
    }
}
