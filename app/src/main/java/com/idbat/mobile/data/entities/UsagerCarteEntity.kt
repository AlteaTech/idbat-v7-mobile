package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "usager_cartes",
    primaryKeys = ["usagerId", "carteId"],
    foreignKeys = [
        ForeignKey(
            entity = UsagerEntity::class,
            parentColumns = ["id"],
            childColumns = ["usagerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CarteContratEntity::class,
            parentColumns = ["id"],
            childColumns = ["carteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usagerId"), Index("carteId")]
)
data class UsagerCarteEntity(
    val usagerId: Long,
    val carteId: Long,
    val dateDebut: Date?,
    val dateFin: Date?
)
