package com.idbat.mobile.data.model

import com.idbat.mobile.data.entities.MatiereSiteEntity

data class SaisieMatiereLigne(
    val matiere: MatiereSiteEntity,
    val quantite: String,
    val usagerNom: String?,
    val numeroCarte: String?,
    val date: Long = System.currentTimeMillis(),
    val siteNom: String
)
