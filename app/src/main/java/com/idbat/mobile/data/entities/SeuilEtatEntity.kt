package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "seuil_etat",
    primaryKeys = ["usagerId", "seuilId"],
    foreignKeys = [
        ForeignKey(
            entity = UsagerEntity::class,
            parentColumns = ["id"],
            childColumns = ["usagerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usagerId")]
)
data class SeuilEtatEntity(
    val usagerId: Long,
    val seuilId: Long,
    val nom: String,
    val nbPassagesAutorises: Int,
    val nbPassagesEffectues: Int,
    val isAlerte: Boolean
)
