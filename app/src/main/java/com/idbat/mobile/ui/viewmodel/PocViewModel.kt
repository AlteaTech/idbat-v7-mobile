package com.idbat.mobile.ui.viewmodel

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.idbat.mobile.data.model.CartePuce
import com.idbat.mobile.data.nfc.NfcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PocUiState(
    val selectedTab: Int = 0,
    val photos: List<Uri> = emptyList(),
    val showCountAlert: Boolean = false,
    val barcodeValue: String? = null,
    val barcodeFormat: String? = null,
    val signatureImage: ImageBitmap? = null,
    val rfidCard: CartePuce? = null,
    val fNumeroId: String = "",
    val fMotDePasse: String = "",
    val fSociete: String = "",
    val fNomPrenom: String = "",
    val fIdentClient: String = "",
    val fSolde: String = "0",
    val fCumul: String = "0",
    val fPaiement: String = "",
    val fInterne: Boolean = false,
    val fPrepaiement: Boolean = false,
    val fFacturation: Boolean = false,
    val fGratuit: Boolean = false,
)

@HiltViewModel
class PocViewModel @Inject constructor(
    val nfcRepository: NfcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PocUiState())
    val uiState: StateFlow<PocUiState> = _uiState.asStateFlow()

    fun selectTab(index: Int) = _uiState.update { it.copy(selectedTab = index) }
    fun setPhotos(photos: List<Uri>) = _uiState.update { it.copy(photos = photos) }
    fun setShowCountAlert(show: Boolean) = _uiState.update { it.copy(showCountAlert = show) }
    fun setBarcodeResult(value: String?, format: String?) =
        _uiState.update { it.copy(barcodeValue = value, barcodeFormat = format) }
    fun setSignatureImage(image: ImageBitmap?) = _uiState.update { it.copy(signatureImage = image) }

    fun onCardRead(card: CartePuce) = _uiState.update {
        it.copy(
            rfidCard     = card,
            fNumeroId    = card.numeroIdentification,
            fMotDePasse  = card.motDePasse,
            fSociete     = card.societe,
            fNomPrenom   = card.nomPrenom,
            fIdentClient = card.identClient,
            fSolde       = card.soldePoints.toBigDecimal().toPlainString(),
            fCumul       = card.cumulPoints.toBigDecimal().toPlainString(),
            fPaiement    = card.paiementComptant,
            fInterne     = card.interne,
            fPrepaiement = card.prepaiement,
            fFacturation = card.facturation,
            fGratuit     = card.gratuit,
        )
    }

    fun setFNumeroId(v: String)     { if (v.length <= 20) _uiState.update { it.copy(fNumeroId = v) } }
    fun setFMotDePasse(v: String)   { if (v.length <= 4)  _uiState.update { it.copy(fMotDePasse = v) } }
    fun setFSociete(v: String)      { if (v.length <= 34) _uiState.update { it.copy(fSociete = v) } }
    fun setFNomPrenom(v: String)    { if (v.length <= 30) _uiState.update { it.copy(fNomPrenom = v) } }
    fun setFIdentClient(v: String)  { if (v.length <= 18) _uiState.update { it.copy(fIdentClient = v) } }
    fun setFSolde(v: String)        { _uiState.update { it.copy(fSolde = v) } }
    fun setFCumul(v: String)        { _uiState.update { it.copy(fCumul = v) } }
    fun setFPaiement(v: String)     { if (v.length <= 70) _uiState.update { it.copy(fPaiement = v) } }
    fun setFInterne(v: Boolean)     { _uiState.update { it.copy(fInterne = v) } }
    fun setFPrepaiement(v: Boolean) { _uiState.update { it.copy(fPrepaiement = v) } }
    fun setFFacturation(v: Boolean) { _uiState.update { it.copy(fFacturation = v) } }
    fun setFGratuit(v: Boolean)     { _uiState.update { it.copy(fGratuit = v) } }
}
