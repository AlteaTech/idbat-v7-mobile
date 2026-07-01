package com.idbat.mobile.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.PassageDocumentEntity
import com.idbat.mobile.data.entities.PassageEntity
import com.idbat.mobile.data.entities.PassageMatiereEntity
import com.idbat.mobile.data.model.SaisieMatiereLigne
import com.idbat.mobile.singleton.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class TerminerSaveState {
    object Idle    : TerminerSaveState()
    object Saving  : TerminerSaveState()
    object Success : TerminerSaveState()
    data class Error(val message: String) : TerminerSaveState()
}

@HiltViewModel
class TerminerPassageViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _saveState = MutableStateFlow<TerminerSaveState>(TerminerSaveState.Idle)
    val saveState: StateFlow<TerminerSaveState> = _saveState.asStateFlow()

    fun terminer(
        contratId: Long,
        siteId: Long,
        carteId: Long?,
        commentaire: String,
        lignes: List<SaisieMatiereLigne>,
        emailSaisi: String?,
        usagerId: Long?,
        photos: List<Uri>,
        signatureImage: ImageBitmap?,
        uidCarte: String? = null,
        soldePointsAvant: Double? = null
    ) {
        if (_saveState.value == TerminerSaveState.Saving) return
        viewModelScope.launch {
            _saveState.value = TerminerSaveState.Saving
            try {
                withContext(Dispatchers.IO) {
                    val userTp = authManager.authState.value.loggedInUtilisateurTp
                        ?: throw IllegalStateException("Utilisateur non connecté")

                    val contrat = database.contratDao().getContratById(contratId)
                        ?: throw IllegalStateException("Contrat introuvable")

                    val site = database.siteDao().getSiteById(siteId)
                        ?: throw IllegalStateException("Site introuvable")

                    val dateHeure = System.currentTimeMillis()
                    val dateSuffix = SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(Date(dateHeure))
                    val numeroBonPassage = "${contrat.trigramme}${site.trigramme}$dateSuffix"
                    val emailSnapshot = emailSaisi?.trim()?.takeIf { it.isNotBlank() }

                    // 1. Enregistrement du passage
                    val passageId = database.passageDao().insertPassage(
                        PassageEntity(
                            dateHeure = dateHeure,
                            contratId = contratId,
                            siteId = siteId,
                            carteId = carteId,
                            userTpId = userTp.id,
                            numeroBonPassage = numeroBonPassage,
                            commentaire = commentaire.takeIf { it.isNotBlank() },
                            emailUsager = emailSnapshot,
                            uidCarte = uidCarte,
                            soldePointsAvant = soldePointsAvant,
                            transactionId = java.util.UUID.randomUUID().toString()
                        )
                    )

                    // 2. Matières
                    database.passageMatiereDao().insertMatieres(
                        lignes.map { ligne ->
                            PassageMatiereEntity(
                                passageId = passageId,
                                matiereId = ligne.matiere.matiereId,
                                siteId = ligne.matiere.siteId,
                                libelle = ligne.matiere.libelle,
                                quantite = ligne.quantite,
                                tarif = ligne.matiere.tarif,
                                unitesLibelle = ligne.matiere.unitesDesApportLibelle
                            )
                        }
                    )

                    // 3. Documents (photos + signature) → base64
                    val documents = mutableListOf<PassageDocumentEntity>()

                    photos.forEachIndexed { index, uri ->
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val ext = mimeType.substringAfterLast("/", "jpg")
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val raw = inputStream.readBytes()
                            inputStream.close()
                            val bytes = resizeToMax1080(raw, mimeType)
                            documents.add(
                                PassageDocumentEntity(
                                    passageId = passageId,
                                    type = "photo",
                                    base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                                    mimeType = mimeType,
                                    nomFichier = "photo_${index + 1}.$ext"
                                )
                            )
                        }
                    }

                    if (signatureImage != null) {
                        val androidBitmap = signatureImage.asAndroidBitmap()
                        val out = ByteArrayOutputStream()
                        androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        val bytes = out.toByteArray()
                        documents.add(
                            PassageDocumentEntity(
                                passageId = passageId,
                                type = "signature",
                                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                                mimeType = "image/png",
                                nomFichier = "signature.png"
                            )
                        )
                    }

                    if (documents.isNotEmpty()) {
                        database.passageDocumentDao().insertDocuments(documents)
                    }

                    // 4. Mise à jour courriel usager (RG5.4 — local, sync BO à prévoir)
                    if (!emailSnapshot.isNullOrBlank() && usagerId != null) {
                        database.usagerDao().updateCouriel(usagerId, emailSnapshot)
                    }
                }

                _saveState.value = TerminerSaveState.Success
            } catch (e: Exception) {
                _saveState.value = TerminerSaveState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun resetState() {
        _saveState.value = TerminerSaveState.Idle
    }

    private fun resizeToMax1080(bytes: ByteArray, mimeType: String): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= 1080) {
            bitmap.recycle()
            return bytes
        }
        val scale = 1080f / maxSide
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
        bitmap.recycle()
        val out = ByteArrayOutputStream()
        val format = if (mimeType.contains("png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        resized.compress(format, 85, out)
        resized.recycle()
        return out.toByteArray()
    }
}
