package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "passage_matiere",
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
data class PassageMatiereEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val passageId: Long,
    val matiereId: Long,
    val siteId: Long,
    val libelle: String,
    val quantite: String,
    val tarif: Double,
    // Valeur de la ligne en points = quantité × tarif
    val points: Double,
    val unitesLibelle: String
)
