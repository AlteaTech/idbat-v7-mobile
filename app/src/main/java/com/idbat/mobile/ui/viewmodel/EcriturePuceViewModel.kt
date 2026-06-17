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
        // Ignore les taps suivants pendant qu'une écriture est déjà en cours
        if (_state.value is WriteState.Writing) return
        _state.value = WriteState.Writing
        viewModelScope.launch {
            try {
                nfcRepository.writeCartePuce(tag, carte)
                _state.value = WriteState.Success(carte.uid)
            } catch (e: Exception) {
                _state.value = WriteState.Error(e.message ?: "Erreur d'écriture")
            }
        }
    }

    fun reset() {
        _state.value = WriteState.Idle
    }
}
