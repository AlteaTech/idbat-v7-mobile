package com.idbat.mobile.data.entities

import androidx.room.Embedded
import androidx.room.Relation

// Classe de relation pour charger un Site et ses Matières
data class SiteWithMatieres(
    @Embedded val site: SiteEntity,
    
    @Relation(
        parentColumn = "id", // L'id dans SiteEntity
        entityColumn = "siteId" // Le siteId dans MatiereSiteEntity
    )
    val matieres: List<MatiereSiteEntity>
)
