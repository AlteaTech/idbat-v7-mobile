package com.idbat.mobile.ui.components

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class CartePuce(
    val uid: String,                  // UID complet pour l'affichage
    val numeroSerie: String,          // 4 premiers bytes en hex sans espaces — utilisé pour le CRC
    val numeroIdentification: String, // offset 0,  20 chars
    val motDePasse: String,           // offset 20,  4 chars
    val societe: String,              // offset 24, 34 chars
    val interne: Boolean,             // offset 58 == '!'
    val prepaiement: Boolean,         // offset 59 hex & 2
    val facturation: Boolean,         // offset 59 hex & 4
    val gratuit: Boolean,             // offset 59 hex & 8
    val nomPrenom: String,            // offset 60, 30 chars
    val identClient: String,          // offset 90, 18 chars
    val soldePoints: Double,          // offset 108, 7 chars / 100
    val cumulPoints: Double,          // offset 115, 7 chars / 100
    val paiementComptant: String,     // offset 122, 70 chars
    val crc: String                   // offset 192, 48 chars
) {
    val isCrcValid: Boolean get() = try {
        computeCrc(numeroSerie, numeroIdentification) == crc.trimEnd()
    } catch (_: Exception) { false }

    /** Sérialise en 240 chars ISO-8859-1 avec CRC recalculé. */
    fun serialize(): String {
        val flags = ((if (prepaiement) 2 else 0) or
                     (if (facturation) 4 else 0) or
                     (if (gratuit)     8 else 0))
            .toString(16).uppercase()
        val crcStr = computeCrc(numeroSerie, numeroIdentification)
        return buildString {
            append(numeroIdentification.take(20).padEnd(20))
            append(motDePasse.take(4).padEnd(4))
            append(societe.take(34).padEnd(34))
            append(if (interne) '!' else ' ')
            append(flags)
            append(nomPrenom.take(30).padEnd(30))
            append(identClient.take(18).padEnd(18))
            append((soldePoints * 100).toLong().toString().padStart(7, '0'))
            append((cumulPoints * 100).toLong().toString().padStart(7, '0'))
            append(paiementComptant.take(70).padEnd(70))
            append(crcStr.take(48).padEnd(48))
        }
    }

    companion object {
        fun parse(uid: String, numeroSerie: String, content: String): CartePuce {
            if (content.length < 240)
                throw Exception("Contenu insuffisant (${content.length}/240 chars)")
            val flagsHex = content[59].digitToIntOrNull(16) ?: 0
            return CartePuce(
                uid = uid,
                numeroSerie = numeroSerie,
                numeroIdentification = content.substring(0, 20).trim(),
                motDePasse = content.substring(20, 24),
                societe = content.substring(24, 58).trim(),
                interne = content[58] == '!',
                prepaiement = (flagsHex and 2) > 0,
                facturation = (flagsHex and 4) > 0,
                gratuit = (flagsHex and 8) > 0,
                nomPrenom = content.substring(60, 90).trim(),
                identClient = content.substring(90, 108).trim(),
                soldePoints = (content.substring(108, 115).trim().toLongOrNull() ?: 0L) / 100.0,
                cumulPoints = (content.substring(115, 122).trim().toLongOrNull() ?: 0L) / 100.0,
                paiementComptant = content.substring(122, 192).trim(),
                crc = content.substring(192, 240)
            )
        }

        /**
         * Réplique exacte du CryptePuce .NET :
         * Triple-DES CBC → Base64 (44 chars) + 4 chars aux positions 10, 21, 32, 43 = 48 chars
         */
        fun computeCrc(numeroSerie: String, numeroIdentification: String): String {
            val input = (numeroSerie + numeroIdentification).toByteArray(Charsets.UTF_8)
            val key = "CLESECURISATIONIDBATPUCE".toByteArray(Charsets.UTF_8)
            val iv  = "Cherche!".toByteArray(Charsets.UTF_8)
            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "DESede"), IvParameterSpec(iv))
            val base64 = Base64.encodeToString(cipher.doFinal(input), Base64.NO_WRAP)
            val extra = buildString {
                for (i in 0..minOf(43, base64.lastIndex))
                    if (i % 11 == 10) append(base64[i])
            }
            return base64 + extra
        }
    }
}
