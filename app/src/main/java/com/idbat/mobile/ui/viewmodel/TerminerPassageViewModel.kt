package com.idbat.mobile.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.nfc.Tag
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
import com.idbat.mobile.data.nfc.NfcRepository
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

/**
 * RG1-RG3 : dépôt payé par carte à puce en pré-paiement — écriture du nouveau solde sur la carte
 * avant enregistrement du passage.
 */
sealed class TerminerWriteState {
    object Idle      : TerminerWriteState()
    object Writing   : TerminerWriteState()
    object WrongCard : TerminerWriteState()               // RG3.1 : carte présentée ≠ carte lue
    object Success   : TerminerWriteState()
    data class Error(val message: String) : TerminerWriteState()
}

@HiltViewModel
class TerminerPassageViewModel @Inject constructor(
    private val database: AppDatabase,
    private val authManager: AuthManager,
    private val nfcRepository: NfcRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _saveState = MutableStateFlow<TerminerSaveState>(TerminerSaveState.Idle)
    val saveState: StateFlow<TerminerSaveState> = _saveState.asStateFlow()

    private val _writeState = MutableStateFlow<TerminerWriteState>(TerminerWriteState.Idle)
    val writeState: StateFlow<TerminerWriteState> = _writeState.asStateFlow()

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
                    persistPassage(
                        contratId, siteId, carteId, commentaire, lignes,
                        emailSaisi, usagerId, photos, signatureImage, uidCarte, soldePointsAvant
                    )
                }
                _saveState.value = TerminerSaveState.Success
            } catch (e: Exception) {
                _saveState.value = TerminerSaveState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * RG1-RG3 : dépôt puce en pré-paiement. Relit la carte présentée pour récupérer la trame
     * complète, vérifie qu'il s'agit bien de la carte lue au départ (RG3.1), écrit le nouveau
     * solde (RG2 : solde de départ − total des points saisis) en conservant tout le reste, puis
     * n'enregistre le passage en base qu'après écriture réussie (RG3.2).
     */
    fun ecrireSoldeEtTerminer(
        tag: Tag,
        contratId: Long,
        siteId: Long,
        carteId: Long?,
        commentaire: String,
        lignes: List<SaisieMatiereLigne>,
        emailSaisi: String?,
        usagerId: Long?,
        photos: List<Uri>,
        signatureImage: ImageBitmap?,
        uidCarteAttendu: String,
        soldePointsAvant: Double,
        nouveauSolde: Double
    ) {
        if (_writeState.value == TerminerWriteState.Writing ||
            _writeState.value == TerminerWriteState.Success
        ) return
        _writeState.value = TerminerWriteState.Writing
        viewModelScope.launch {
            try {
                // Relecture de la carte présentée (trame complète nécessaire pour la réécriture)
                val carte = withContext(Dispatchers.IO) { nfcRepository.readCartePuce(tag) }

                // RG3.1 : même carte que celle lue au début du dépôt (UID identique)
                if (carte.uid != uidCarteAttendu) {
                    _writeState.value = TerminerWriteState.WrongCard
                    return@launch
                }

                // RG2 : nouveau solde = solde de départ − total des points. Tout le reste inchangé.
                // RG2.1 (#238) : le dépassement du solde est bloqué en amont (SaisieMatiereScreen),
                // donc nouveauSolde ne devrait jamais être négatif ici.
                val carteMaj = carte.copy(soldePoints = nouveauSolde)

                withContext(Dispatchers.IO) {
                    // Écriture NFC (le CRC est recalculé dans serialize())
                    nfcRepository.writeCartePuce(tag, carteMaj)

                    // RG3.2 : enregistrement du passage seulement après écriture réussie
                    persistPassage(
                        contratId, siteId, carteId, commentaire, lignes,
                        emailSaisi, usagerId, photos, signatureImage, uidCarteAttendu, soldePointsAvant
                    )
                }

                _writeState.value = TerminerWriteState.Success
            } catch (e: Exception) {
                _writeState.value = TerminerWriteState.Error(e.message ?: "Erreur d'écriture")
            }
        }
    }

    /** Persistance commune (passage + matières + documents + courriel). À appeler sur IO. */
    private suspend fun persistPassage(
        contratId: Long,
        siteId: Long,
        carteId: Long?,
        commentaire: String,
        lignes: List<SaisieMatiereLigne>,
        emailSaisi: String?,
        usagerId: Long?,
        photos: List<Uri>,
        signatureImage: ImageBitmap?,
        uidCarte: String?,
        soldePointsAvant: Double?
    ) {
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

    fun resetState() {
        _saveState.value = TerminerSaveState.Idle
    }

    fun resetWrite() {
        _writeState.value = TerminerWriteState.Idle
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
