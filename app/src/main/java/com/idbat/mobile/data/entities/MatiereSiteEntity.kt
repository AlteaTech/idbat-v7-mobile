package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// Cette table est une table de jointure. 
// La clé primaire est composée de la paire (matiereId, siteId)
// pour garantir qu'une matière n'est associée qu'une seule fois à un site.
@Entity(
    tableName = "matieres_site", // Nom de table au pluriel pour la cohérence
    primaryKeys = ["matiereId", "siteId"],
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
        // Note: On pourrait aussi ajouter une clé étrangère vers une table "Matieres" si elle existait.
    ],
    indices = [Index("siteId")]
)
data class MatiereSiteEntity(
    // L'ID de la matière elle-même, venant de l'API
    val matiereId: Long,
    
    // L'ID du site auquel elle est liée
    val siteId: Long,

    // Le libellé de la matière, stocké ici pour un accès facile
    val libelle: String,
    val unitesDesApportId: Long,
    val unitesDesApportLibelle: String
)
