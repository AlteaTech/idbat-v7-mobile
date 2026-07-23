package com.idbat.mobile.data.model

data class InfoCartePassage(
    val societe: String? = null,
    val nomTitulaire: String? = null,
    val numeroCarte: String? = null,
    val typeApporteur: String? = null,
    val contact: String? = null,
    val carteId: Long? = null,
    val typeApporteurIsPro: Boolean? = null,
    val usagerId: Long? = null,
    // Carte à puce : UID physique lu + solde de points (null pour les autres types de carte)
    val uid: String? = null,
    val soldePoints: Double? = null
)
