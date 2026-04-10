package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sites",
    foreignKeys = [
        ForeignKey(
            entity = ContratEntity::class,
            parentColumns = ["id"],
            childColumns = ["contratId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contratId")] // Un index sur la clé étrangère est recommandé par Room
)
data class SiteEntity(
    @PrimaryKey
    val id: Long,
    val trigramme: String,
    val nom: String,
    val adresse1: String?,
    val adresse2: String?,
    val codePostal: String?,
    val ville: String?,
    val typeImprimante: String?,
    val macImprimante: String?,
    val horairesOuverture: String?,
    val destinatairesMailTransfertTP: String?,
    val contratId: Long // Clé étrangère vers contrats.id
)
