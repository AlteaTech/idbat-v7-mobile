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
    val isAlerte: Boolean,
    // #487 : le back distingue l'avertissement (isAlerte) du blocage (isBloquant, rechargements inclus)
    val isBloquant: Boolean = false,
    val seuilDetailNom: String,
    val seuilDetailType: String,
    val seuilDetailPeriode: String,
    val seuilDetailNbPassage: Int?,
    val seuilDetailSeuilPrevention: Int,
    val seuilDetailContratId: Long
)
