package com.idbat.mobile.data.model

/**
 * #533 : immatriculation inconnue présentée sur un contrat qui accepte des passages inconnus
 * (`nbPassagesInconnusAcceptes > 0`). `motifAutorisationContratId` null = 1er passage.
 */
data class PassageInconnuInfo(
    val immatriculation: String,
    // Passages déjà enregistrés pour cette immatriculation (0 au 1er passage)
    val nbPassagesDeja: Int,
    val nbPassagesAutorises: Int,
    // Motif choisi au 1er passage (null = 1er passage → liste déroulante obligatoire, RG2.1)
    val motifAutorisationContratId: Long? = null,
    val motifAutorisationLibelle: String? = null
) {
    val isPremierPassage: Boolean get() = motifAutorisationContratId == null
    // RG8 : nombre de passages exceptionnels autorisés atteint → refus
    val isMaxAtteint: Boolean get() = nbPassagesDeja >= nbPassagesAutorises
}
