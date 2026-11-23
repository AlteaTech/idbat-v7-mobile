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
    val hasSignatureProfessionnels: Boolean = false,
    val hasAccessSimpleParticuliers: Boolean = false,
    val hasAccessSimpleProfessionnels: Boolean = false,
    val hasPrepaiementParticuliers: Boolean = false,
    val hasPaimentComptantParticuliers: Boolean = false,
    val hasPrepaiementProfessionnels: Boolean = false,
    val hasGratuitParticuliers: Boolean = false,
    val hasGratuitProfessionels: Boolean = false,
    val hasPaimentComptantProfessionels: Boolean = false,
    // #489 RG1 : décompte des unités de passage (cartes immatriculation)
    val hasDecompteunites: Boolean = false,
    // #533 : nb de passages acceptés pour une immatriculation inconnue (0 = fonctionnalité inactive)
    val nbPassagesInconnusAcceptes: Int = 0
)
