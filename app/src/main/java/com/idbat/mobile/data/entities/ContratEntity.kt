package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contrats")
data class ContratEntity(
    @PrimaryKey
    val id: Long,
    val trigramme: String,
    val nom: String,
    val hasPuce: Boolean = false,
    val hasCodebarres: Boolean = false,
    val hasImmatriculation: Boolean = false,
    val hasSelectionusager: Boolean = false,
    val hasSignatureParticuliers: Boolean = false,
    val hasSignatureProfessionnels: Boolean = false
)
