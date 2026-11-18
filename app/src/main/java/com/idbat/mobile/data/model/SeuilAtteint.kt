package com.idbat.mobile.data.model

import com.idbat.mobile.data.entities.CarteContratEntity
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.SeuilEtatEntity

/**
 * #513 — Seuil atteint pour la carte présentée (prise en compte des unités de passage).
 *
 * Miroir de `SeuilAtteintHelper.estAtteintPourCarte` côté back (#512, barrières) :
 * ```
 * atteint = !isAlerte
 *        || (hasDecompteUnites && carte.type == "I" && carte.nbUnitesADecompter != null
 *            && carte.nbUnitesADecompter > nbPassagesAutorises - nbPassagesEffectues)
 * ```
 * - RG1 : uniquement si `contrat.hasDecompteunites`.
 * - RG2 : uniquement pour une carte de type 'I' (immatriculation) ; `nbUnitesADecompter` null → hors règle.
 * - RG3 : unités de la carte STRICTEMENT supérieures aux unités restantes → seuil considéré atteint
 *   (picto/comportement d'un seuil atteint, bloquant si `isBloquant`, libellé « Seuil X atteint… »).
 */
object SeuilAtteint {

    /** RG3 seule : seuil encore en prévention (`isAlerte`) mais carte trop « lourde » pour passer. */
    fun estAtteintParUnites(
        seuil: SeuilEtatEntity,
        contrat: ContratEntity?,
        carte: CarteContratEntity?
    ): Boolean {
        if (!seuil.isAlerte) return false
        if (contrat?.hasDecompteunites != true) return false                       // RG1
        if (carte?.type != DecompteUnites.TYPE_CARTE_IMMATRICULATION) return false  // RG2
        val unitesCarte = carte.nbUnitesADecompter ?: return false
        val unitesRestantes = seuil.nbPassagesAutorises - seuil.nbPassagesEffectues
        return unitesCarte > unitesRestantes                                        // RG3
    }

    /** Seuil atteint pour ce passage : plafond déjà atteint (`!isAlerte`) OU RG3. */
    fun estAtteint(seuil: SeuilEtatEntity, contrat: ContratEntity?, carte: CarteContratEntity?): Boolean =
        !seuil.isAlerte || estAtteintParUnites(seuil, contrat, carte)

    /**
     * RG3.2 : libellé « seuil atteint » (équivalent local de `SeuilEtatDmo.messageAtteint`),
     * affiché même si nbPassagesEffectues < nbPassagesAutorises.
     */
    fun messageAtteint(seuil: SeuilEtatEntity): String =
        "Seuil ${seuil.nom} atteint, ${seuil.nbPassagesEffectues} passages ${periode(seuil)} pour ${seuil.nbPassagesAutorises} autorisés"

    /** Libellé de prévention (seuil non atteint). */
    fun messagePrevention(seuil: SeuilEtatEntity): String =
        "Le seuil ${seuil.nom} autorise ${seuil.nbPassagesAutorises} passages ${periode(seuil)}, ${seuil.nbPassagesEffectues} déjà effectués"

    /** Libellé affiché pour ce passage, selon l'état « atteint » évalué pour la carte présentée. */
    fun message(seuil: SeuilEtatEntity, atteint: Boolean): String =
        if (atteint) messageAtteint(seuil) else messagePrevention(seuil)

    /**
     * Même phrase que le back (UsagersMapper.periodePhrase). `seuilDetailPeriode` arrive déjà traduit
     * (« Annuel », « Quotidien »…) ; les codes bruts (A/S/T/M/Q) sont aussi acceptés.
     */
    private fun periode(seuil: SeuilEtatEntity): String = when (seuil.seuilDetailPeriode.trim().lowercase()) {
        "annuel", "a" -> "sur l'année"
        "semestriel", "s" -> "sur le semestre"
        "trimestriel", "t" -> "sur le trimestre"
        "mensuel", "m" -> "sur le mois"
        "quotidien", "q" -> "sur la journée"
        else -> ""
    }

    /**
     * Mise à jour locale d'un seuil après un passage enregistré, en attendant la synchro (le back reste
     * la source de vérité : la descendante écrase tout). Mêmes règles que le back :
     * - `isAlerte = nbPassagesEffectues < nbPassagesAutorises` (SeuilsUsagersService) ;
     * - `isPreventionAtteinte = !isAlerte || nbPassagesEffectues >= floor(prévention × autorisés / 100)` ;
     * - `messageAtteint` du back périmé (il contient l'ancien compteur) → `null`, repli sur le libellé local.
     */
    /** Seuil qui compte des passages (type 'P', transmis « Periode » par le back) — seuls ceux-là sont incrémentés. */
    fun comptePassages(seuil: SeuilEtatEntity): Boolean =
        seuil.seuilDetailType.equals("Periode", ignoreCase = true) || seuil.seuilDetailType.equals("P", ignoreCase = true)

    fun apresPassage(seuil: SeuilEtatEntity, nbUnitesDecomptees: Int): SeuilEtatEntity {
        val effectues = seuil.nbPassagesEffectues + nbUnitesDecomptees
        val isAlerte = effectues < seuil.nbPassagesAutorises
        val valeurPrevention = Math.floorDiv(seuil.seuilDetailSeuilPrevention * seuil.nbPassagesAutorises, 100)
        return seuil.copy(
            nbPassagesEffectues = effectues,
            isAlerte = isAlerte,
            isPreventionAtteinte = !isAlerte || effectues >= valeurPrevention,
            messageAtteint = null
        )
    }
}

/**
 * Seuil de l'usager évalué pour la carte présentée à ce passage (#513).
 *
 * - atteint (`!isAlerte` ou RG3) → message « atteint », bloquant si `isBloquant`, remonté dans seuilsAvertis
 *   (y compris quand `isPreventionAtteinte = false`) ;
 * - sinon `isPreventionAtteinte` → avertissement de prévention (inchangé) ;
 * - sinon → **invisible** : seuil transmis seulement pour connaître les unités restantes.
 */
data class SeuilEvalue(
    val seuil: SeuilEtatEntity,
    /** Atteint pour ce passage (`!isAlerte` OU RG3). */
    val atteint: Boolean,
    /** Atteint uniquement à cause de RG3 (isAlerte = true mais carte trop lourde). */
    val atteintParUnites: Boolean
) {
    val bloque: Boolean get() = atteint && seuil.isBloquant

    /** À afficher (message + picto) : atteint, ou dans la zone de prévention. */
    val visible: Boolean get() = atteint || seuil.isPreventionAtteinte

    /**
     * Libellé affiché. RG3 : `messageAtteint` du back (période exacte : « sur la journée »…), repli local
     * pour une ligne antérieure à la colonne. Hors RG3 : libellé local historique, inchangé.
     */
    val message: String
        get() = if (atteintParUnites) {
            seuil.messageAtteint?.takeIf { it.isNotBlank() } ?: SeuilAtteint.messageAtteint(seuil)
        } else {
            SeuilAtteint.message(seuil, atteint)
        }

    companion object {
        fun evaluer(seuil: SeuilEtatEntity, contrat: ContratEntity?, carte: CarteContratEntity?): SeuilEvalue {
            val parUnites = SeuilAtteint.estAtteintParUnites(seuil, contrat, carte)
            return SeuilEvalue(seuil, atteint = !seuil.isAlerte || parUnites, atteintParUnites = parUnites)
        }
    }
}
