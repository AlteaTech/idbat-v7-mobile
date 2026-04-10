package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "motifs_liste_noire_contrat",
    foreignKeys = [
        ForeignKey(
            entity = ContratEntity::class,
            parentColumns = ["id"],
            childColumns = ["contratId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contratId")]
)
data class MotifListeNoireContratEntity(
    @PrimaryKey
    val id: Long,
    val libelle: String,
    val contratId: Long,
    val motifListeNoireId: Long
)
