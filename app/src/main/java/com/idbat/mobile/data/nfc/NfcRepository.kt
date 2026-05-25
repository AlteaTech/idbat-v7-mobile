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

@Singleton
class NfcRepository @Inject constructor() {

    suspend fun readCartePuce(tag: Tag): CartePuce = withContext(Dispatchers.IO) {
        val uid = tag.id.joinToString(" ") { "%02X".format(it) }
        val numeroSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }

        if (MifareClassic::class.java.name !in tag.techList)
            throw Exception("Carte non supportée (Mifare Classic requis)")

        val mifare = MifareClassic.get(tag) ?: throw Exception("Impossible d'accéder à la carte")
        val contentBytes = ByteArray(240)

        data class Block(val sector: Int, val offset: Int)
        val sequence = buildList {
            add(Block(0, 1)); add(Block(0, 2))
            for (s in 1..4) { add(Block(s, 0)); add(Block(s, 1)); add(Block(s, 2)) }
            add(Block(5, 0))
        }

        mifare.use { card ->
            card.connect()
            var authenticatedSector = -1
            sequence.forEachIndexed { i, blk ->
                if (blk.sector != authenticatedSector) {
                    val ok = card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_DEFAULT)
                        || card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                        || card.authenticateSectorWithKeyB(blk.sector, MifareClassic.KEY_DEFAULT)
                    if (!ok) throw Exception("Authentification échouée — secteur ${blk.sector}")
                    authenticatedSector = blk.sector
                }
                card.readBlock(card.sectorToBlock(blk.sector) + blk.offset)
                    .copyInto(contentBytes, i * 16)
            }
        }

        Log.d(TAG, "Lecture OK — UID $uid")
        CartePuce.parse(uid, numeroSerie, String(contentBytes, Charsets.ISO_8859_1))
    }

    suspend fun writeCartePuce(tag: Tag, carte: CartePuce) = withContext(Dispatchers.IO) {
        val content = carte.serialize()
        check(content.length == 240) { "Sérialisation invalide (${content.length}/240)" }
        val bytes = content.toByteArray(Charsets.ISO_8859_1)

        data class Block(val sector: Int, val offset: Int)
        val sequence = buildList {
            add(Block(0, 1)); add(Block(0, 2))
            for (s in 1..4) { add(Block(s, 0)); add(Block(s, 1)); add(Block(s, 2)) }
            add(Block(5, 0))
        }

        val mifare = MifareClassic.get(tag) ?: throw Exception("Carte non supportée")

        mifare.use { card ->
            card.connect()
            var authenticatedSector = -1

            sequence.forEachIndexed { i, blk ->
                if (blk.sector != authenticatedSector) {
                    val ok = card.authenticateSectorWithKeyB(blk.sector, MifareClassic.KEY_DEFAULT)
                        || card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_DEFAULT)
                        || card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                    if (!ok) throw Exception("Authentification échouée — secteur ${blk.sector}")
                    authenticatedSector = blk.sector
                }

                val blockIndex = card.sectorToBlock(blk.sector) + blk.offset
                val data = bytes.sliceArray(i * 16 until (i + 1) * 16)

                Log.d(TAG, "writeBlock($blockIndex) [S${blk.sector}B${blk.offset}]")
                card.writeBlock(blockIndex, data)

                val readBack = card.readBlock(blockIndex)
                if (!readBack.contentEquals(data))
                    throw Exception("Vérification échouée — S${blk.sector}B${blk.offset}")
            }
        }

        Log.d(TAG, "Écriture OK — UID ${carte.uid}")
    }
}
