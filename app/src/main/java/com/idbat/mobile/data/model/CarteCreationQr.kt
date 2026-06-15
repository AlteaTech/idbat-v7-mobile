package com.idbat.mobile.data.model

/**
 * Contenu d'un QR code de création de carte à puce.
 * Trame fixe de 192 caractères, découpée par offsets :
 *
 * | Champ                  | Offset    | Longueur |
 * |------------------------|-----------|----------|
 * | NUMEROIDENTIFICATION   | 0..20     | 20       |
 * | MOTDEPASSE             | 20..24    | 4        |
 * | SOCIETE                | 24..58    | 34       |
 * | INTERNE                | 58..59    | 1        |
 * | MODESPAIEMENT (hex)    | 59..60    | 1        |
 * | NOMPRENOM              | 60..90    | 30       |
 * | IDENTCLIENT            | 90..108   | 18       |
 * | SOLDEPOINTS            | 108..115  | 7        |
 * | CUMULPOINTS            | 115..122  | 7        |
 * | ANCIEN_PAIEMENT        | 122..192  | 70       |
 */
data class CarteCreationQr(
    val numeroIdentification: String,
    val motDePasse: String,
    val societe: String,
    val interne: String,
    val modesPaiementHex: String,
    val nomPrenom: String,
    val identClient: String,
    val soldePoints: String,
    val cumulPoints: String
) {
    // Décodage des modes de paiement (hex : prépaiement=2, facturation=4, gratuit=8)
    private val modeValue: Int = modesPaiementHex.trim().toIntOrNull(16) ?: 0
    val prepaiement: Boolean get() = modeValue and 0x2 != 0
    val facturation: Boolean get() = modeValue and 0x4 != 0
    val gratuit: Boolean     get() = modeValue and 0x8 != 0

    companion object {
        const val TRAME_LENGTH = 192

        fun parse(raw: String): CarteCreationQr? {
            if (raw.length < TRAME_LENGTH) return null
            val s = raw.substring(0, TRAME_LENGTH)
            return CarteCreationQr(
                numeroIdentification = s.substring(0, 20).trim(),
                motDePasse           = s.substring(20, 24),
                societe              = s.substring(24, 58).trim(),
                interne              = s.substring(58, 59),
                modesPaiementHex     = s.substring(59, 60),
                nomPrenom            = s.substring(60, 90).trim(),
                identClient          = s.substring(90, 108).trim(),
                soldePoints          = s.substring(108, 115),
                cumulPoints          = s.substring(115, 122)
            )
        }
    }
}
