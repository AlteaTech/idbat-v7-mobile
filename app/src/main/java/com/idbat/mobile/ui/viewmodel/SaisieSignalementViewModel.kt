package com.idbat.mobile.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.ContratEvenementEntity
import com.idbat.mobile.data.entities.SignalementDocumentEntity
import com.idbat.mobile.data.entities.SignalementEntity
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class SaisieSignalementViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager
) : ViewModel() {

    data class UiState(
        val evenements: List<ContratEvenementEntity> = emptyList(),
        val selectedEvenement: ContratEvenementEntity? = null,
        val commentaire: String = "",
        val isSubmitting: Boolean = false,
        val submitSuccess: Boolean = false,
        val submitError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun loadEvenements(contratId: Long) {
        viewModelScope.launch {
            database.contratEvenementDao()
                .getEvenementsByContratFlow(contratId)
                .collect { list ->
                    _uiState.value = _uiState.value.copy(
                        evenements = list,
                        selectedEvenement = _uiState.value.selectedEvenement ?: list.firstOrNull()
                    )
                }
        }
    }

    fun selectEvenement(evenement: ContratEvenementEntity) {
        _uiState.value = _uiState.value.copy(selectedEvenement = evenement)
    }

    fun setCommentaire(text: String) {
        if (text.length <= 50) _uiState.value = _uiState.value.copy(commentaire = text)
    }

    fun soumettre(siteId: Long, contratId: Long, photos: List<Uri>, context: Context) {
        val event = _uiState.value.selectedEvenement ?: return
        // L'utilisateur connecté (TP) est lu à la source pour être persisté de façon fiable
        val userTp = authManager.authState.value.loggedInUtilisateurTp
        if (userTp == null) {
            _uiState.value = _uiState.value.copy(submitError = "Utilisateur non connecté")
            return
        }
        _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Enregistrement du signalement (outbox local)
                val signalementId = database.signalementDao().insert(
                    SignalementEntity(
                        siteId             = siteId,
                        contratId          = contratId,
                        evenementContratId = event.jointureId,
                        agentId            = userTp.id,
                        dateSignalement    = System.currentTimeMillis(),
                        commentaire        = _uiState.value.commentaire.takeIf { it.isNotBlank() }
                    )
                )

                // 2. Photos associées → base64 (resize 1080)
                val docs = photos.mapIndexed { i, uri ->
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = mimeType.substringAfterLast("/", "jpg")
                    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                    SignalementDocumentEntity(
                        signalementId = signalementId,
                        base64        = Base64.encodeToString(resizeToMax(raw, mimeType), Base64.NO_WRAP),
                        mimeType      = mimeType,
                        nomFichier    = "photo_${i + 1}.$ext"
                    )
                }
                if (docs.isNotEmpty()) {
                    database.signalementDocumentDao().insertDocuments(docs)
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError  = e.message ?: "Erreur inattendue"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(submitError = null)
    }

    /** Réinitialise le formulaire après un envoi réussi (le VM est scopé à la route Home). */
    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(
            submitSuccess = false,
            commentaire = "",
            selectedEvenement = null
        )
    }

    private fun resizeToMax(bytes: ByteArray, mimeType: String): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= 4000) { bitmap.recycle(); return bytes }
        val scale = 4000f / maxSide
        val resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        bitmap.recycle()
        val out = ByteArrayOutputStream()
        val format = if (mimeType.contains("png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        resized.compress(format, 85, out)
        resized.recycle()
        return out.toByteArray()
    }
}
