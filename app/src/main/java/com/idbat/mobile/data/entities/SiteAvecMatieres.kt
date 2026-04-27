package com.idbat.mobile.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class SiteAvecMatieres(
    @Embedded val site: SiteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "siteId"
    )
    val matieres: List<MatiereSiteEntity>
)
