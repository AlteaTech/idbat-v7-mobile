package com.idbat.mobile.ui.viewmodel

import android.graphics.*
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.PassageDocumentEntity
import com.idbat.mobile.data.entities.PassageEntity
import com.idbat.mobile.data.entities.PassageMatiereEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TestVolumeViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val message: String) : State()
        data class Done(val totalPassages: Int, val totalSites: Int, val totalUsers: Int) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun generer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = State.Running("Initialisation...")

                val sites       = database.siteDao().getAllSitesFlow().first()
                val utilisateurs = database.utilisateurTPDao().getAllUtilisateurs()

                // Générer les images factices une seule fois (références réutilisées)
                val fakePhoto1    = fakePhoto(Color.rgb(180, 60, 60))
                val fakePhoto2    = fakePhoto(Color.rgb(60, 100, 180))
                val fakeSignature = fakeSignature()

                var total = 0

                for (site in sites) {
                    val matieres = database.matiereSiteDao().getMatieresBySite(site.id)
                    val cartes   = database.carteContratDao().getCartesByContrat(site.contratId)

                    for (user in utilisateurs) {
                        _state.value = State.Running(
                            "Site « ${site.nom} » · ${user.login}\n${total} passages créés…"
                        )

                        // 1. Construire + insérer les 100 passages par lot de 100
                        val passageIds = mutableListOf<Long>()
                        (1..100).chunked(100).forEach { chunk ->
                            val batch = chunk.map { i ->
                                // Round-robin sur les cartes disponibles, null si aucune
                                val carteId = cartes.getOrNull(i % cartes.size)?.id
                                PassageEntity(
                                    dateHeure        = System.currentTimeMillis() - (i * 60_000L),
                                    contratId        = site.contratId,
                                    siteId           = site.id,
                                    carteId          = carteId,
                                    userTpId         = user.id,
                                    numeroBonPassage = "VOL${site.trigramme.take(3)}${i.toString().padStart(4, '0')}",
                                    transactionId    = UUID.randomUUID().toString()
                                )
                            }
                            passageIds.addAll(database.passageDao().insertPassages(batch))
                        }

                        // 2. Matières (toutes les matières du site pour chaque passage)
                        if (matieres.isNotEmpty()) {
                            passageIds.chunked(200).forEach { idBatch ->
                                val rows = idBatch.flatMap { pid ->
                                    matieres.map { m ->
                                        PassageMatiereEntity(
                                            passageId    = pid,
                                            matiereId    = m.matiereId,
                                            siteId       = site.id,
                                            libelle      = m.libelle,
                                            quantite     = (1..100).random().toString(),
                                            tarif        = m.tarif,
                                            unitesLibelle = m.unitesDesApportLibelle
                                        )
                                    }
                                }
                                database.passageMatiereDao().insertMatieres(rows)
                            }
                        }

                        // 3. Documents : 2 photos + 1 signature par passage
                        passageIds.chunked(200).forEach { idBatch ->
                            val docs = idBatch.flatMap { pid ->
                                listOf(
                                    PassageDocumentEntity(0, pid,"photo",     fakePhoto1,    "image/jpeg", "photo_1.jpg"),
                                    PassageDocumentEntity(0, pid,"photo",     fakePhoto2,    "image/jpeg", "photo_2.jpg"),
                                    PassageDocumentEntity(0, pid,"signature", fakeSignature, "image/png",  "signature.png")
                                )
                            }
                            database.passageDocumentDao().insertDocuments(docs)
                        }

                        total += 1000
                    }
                }

                _state.value = State.Done(
                    totalPassages = total,
                    totalSites    = sites.size,
                    totalUsers    = utilisateurs.size
                )

            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
    }

    private fun fakePhoto(color: Int): String {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
        bmp.eraseColor(color)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
        bmp.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun fakeSignature(): String {
        val bmp = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color       = Color.BLACK
            strokeWidth = 4f
            style       = Paint.Style.STROKE
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(20f, 50f)
            cubicTo(80f, 10f, 160f, 90f, 240f, 40f)
            cubicTo(300f, 10f, 350f, 70f, 380f, 50f)
        }
        canvas.drawPath(path, paint)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
