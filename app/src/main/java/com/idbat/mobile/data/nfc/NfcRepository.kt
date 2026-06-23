package com.idbat.mobile.data.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import com.idbat.mobile.data.model.CartePuce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NfcRepository"

/**
 * Lecture / écriture des cartes à puce Mifare Classic — réplique exacte du device .NET
 * (cf. `ecriture-lecture.cs` à la racine).
 *
 * Cartographie : secteurs 1..10, blocs 0/1/2 (le trailer bloc 3 et tout le secteur 0 ne sont
 * jamais touchés). 30 blocs × 8 chars logiques = 240 chars (= `CartePuce.serialize()`).
 *
 * Encodage on-card (hex-ASCII) : chaque caractère logique est stocké sur 2 octets = les 2
 * chiffres hexa ASCII de son code (ex. 'A' = 0x41 → octets '4','1' = 0x34,0x31). Donc
 * 8 chars logiques → 16 octets par bloc. La lecture fait l'inverse.
 */
@Singleton
class NfcRepository @Inject constructor() {

    suspend fun readCartePuce(tag: Tag): CartePuce = withContext(Dispatchers.IO) {
        val uid = tag.id.joinToString(" ") { "%02X".format(it) }
        val numeroSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }

        if (MifareClassic::class.java.name !in tag.techList)
            throw Exception("Carte non supportée (Mifare Classic requis)")

        val mifare = MifareClassic.get(tag) ?: throw Exception("Impossible d'accéder à la carte")
        val contenu = StringBuilder()

        mifare.use { card ->
            card.connect()

            // Secteurs 1..8 : lecture stricte.
            for (sector in 1..8) {
                if (!card.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT))
                    throw Exception("Authentification échouée — secteur $sector")
                for (block in 0..2) {
                    val raw = card.readBlock(card.sectorToBlock(sector) + block)
                    contenu.append(decodeBlock(raw))
                }
            }

            // Secteurs 9..10 : tolérant (bloc illisible → 8 espaces), comme le .NET.
            for (sector in 9..10) {
                val authed = card.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT)
                for (block in 0..2) {
                    contenu.append(
                        if (!authed) BLOC_VIDE
                        else try {
                            decodeBlock(card.readBlock(card.sectorToBlock(sector) + block))
                        } catch (_: Exception) {
                            BLOC_VIDE
                        }
                    )
                }
            }
        }

        Log.d(TAG, "Lecture OK — UID $uid")
        CartePuce.parse(uid, numeroSerie, contenu.toString())
    }

    suspend fun writeCartePuce(tag: Tag, carte: CartePuce) = withContext(Dispatchers.IO) {
        val content = carte.serialize().padEnd(240).take(240)
        check(content.length == 240) { "Sérialisation invalide (${content.length}/240)" }

        val mifare = MifareClassic.get(tag) ?: throw Exception("Carte non supportée")

        mifare.use { card ->
            card.connect()

            for (sector in 1..10) {
                if (!card.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT))
                    throw Exception("Authentification échouée — secteur $sector")

                for (block in 0..2) {
                    val chunkIndex = (sector - 1) * 3 + block
                    val chunk = content.substring(chunkIndex * 8, chunkIndex * 8 + 8)
                    val data = encodeBlock(chunk)

                    val blockIndex = card.sectorToBlock(sector) + block
                    Log.d(TAG, "writeBlock($blockIndex) [S${sector}B${block}]")
                    card.writeBlock(blockIndex, data)

                    val readBack = card.readBlock(blockIndex)
                    if (!readBack.contentEquals(data))
                        throw Exception("Vérification échouée — S${sector}B${block}")
                }
            }
        }

        Log.d(TAG, "Écriture OK — UID ${carte.uid}")
    }

    private companion object {
        /** 8 caractères logiques illisibles → 8 espaces (cf. lecture tolérante .NET). */
        const val BLOC_VIDE = "        "

        /**
         * 8 chars logiques → 16 octets : chaque char devient les 2 chiffres hexa ASCII de
         * son code (ex. 'A' 0x41 → '4','1' = 0x34,0x31).
         */
        fun encodeBlock(chunk: String): ByteArray {
            val out = ByteArray(16)
            var cpt = 0
            for (c in chunk) {
                val hex = c.code.toString(16).uppercase().padStart(2, '0')
                out[cpt] = hex[0].code.toByte()
                out[cpt + 1] = hex[1].code.toByte()
                cpt += 2
            }
            return out
        }

        /** 16 octets (chiffres hexa ASCII) → 8 chars logiques. */
        fun decodeBlock(raw: ByteArray): String {
            val s = String(raw, Charsets.ISO_8859_1)
            return buildString {
                var k = 0
                while (k < 16) {
                    append(s.substring(k, k + 2).toInt(16).toChar())
                    k += 2
                }
            }
        }
    }
}
