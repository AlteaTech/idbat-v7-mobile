package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * #487/#484 : seuils pour lesquels l'usager a été averti (isAlerte) et/ou bloqué (isBloquant)
 * au moment du passage. Capturés à l'enregistrement (état affiché au technicien) puis remontés
 * au BO dans `CreerPassageRequest.seuilsAvertis`. FK CASCADE : purgés avec le passage.
 */
@Entity(
    tableName = "passage_seuil",
    foreignKeys = [
        ForeignKey(
            entity = PassageEntity::class,
            parentColumns = ["id"],
            childColumns = ["passageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("passageId")]
)
data class PassageSeuilEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val passageId: Long,
    val seuilId: Long,
    val libelle: String,
    val isAlerte: Boolean,
    val isBloquant: Boolean
)
