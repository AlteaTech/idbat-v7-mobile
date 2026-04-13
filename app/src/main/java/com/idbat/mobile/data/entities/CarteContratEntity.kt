package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "carte_contrat",
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
data class CarteContratEntity(
    @PrimaryKey
    val id: Long,
    val libelle: String,
    val type: String?,
    val valeur: String?,
    val uidRfid: String?,
    val isCreationByQRCode: Boolean,
    val carteGriseJ1: String?,
    val carteGriseF3: Int?,
    val contratId: Long,
    val carteId: Long
)
