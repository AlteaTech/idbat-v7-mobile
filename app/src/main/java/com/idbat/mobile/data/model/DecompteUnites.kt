package com.idbat.mobile.data.model

import com.idbat.mobile.data.entities.CarteContratEntity
import com.idbat.mobile.data.entities.ContratEntity

/**
 * #489 — Décompte des unités de passage.
 *
 * - RG1 : ne s'applique que si `contrat.hasDecompteunites`.
 * - RG2 : ne s'applique qu'aux cartes de type 'I' (immatriculation).
 * - RG4 : la valeur stockée sur le passage = `carte.nbUnitesADecompter`.
 * - RG4.2 : dans tous les autres cas (ou valeur carte absente / < 1) → 1.
 */
object DecompteUnites {
    const val TYPE_CARTE_IMMATRICULATION = "I"
    const val NB_UNITES_DEFAUT = 1

    fun nbUnitesDecomptees(contrat: ContratEntity?, carte: CarteContratEntity?): Int {
        if (contrat?.hasDecompteunites != true) return NB_UNITES_DEFAUT
        if (carte?.type != TYPE_CARTE_IMMATRICULATION) return NB_UNITES_DEFAUT
        return carte.nbUnitesADecompter?.takeIf { it >= 1 } ?: NB_UNITES_DEFAUT
    }
}
