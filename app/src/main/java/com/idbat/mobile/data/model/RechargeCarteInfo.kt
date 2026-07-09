package com.idbat.mobile.data.model

/**
 * Infos affichées sur l'écran de rechargement après lecture d'une carte à puce.
 * Combine les données lues sur la carte (solde) et celles de l'usager lié en BDD.
 */
data class RechargeCarteInfo(
    val societe: String? = null,
    val nomTitulaire: String? = null,
    val numeroCarte: String? = null,
    val typeApporteur: String? = null,
    val contact: String? = null,
    val soldePoints: Double = 0.0,
    val typeApporteurIsPro: Boolean = false
)
