package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * #533 RG2.1 : motif d'autorisation exceptionnelle (libellé personnalisé pour le contrat), proposé
 * au 1er passage d'une immatriculation inconnue. Sans FK : vidé/réinséré à chaque synchro descendante.
 */
@Entity(
    tableName = "motif_autorisation_contrat",
    indices = [Index("contratId")]
)
data class MotifAutorisationContratEntity(
    @PrimaryKey
    val id: Long,
    val contratId: Long,
    val libelle: String
)
