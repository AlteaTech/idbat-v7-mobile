package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Paramètre global applicatif (couple clef/valeur + description optionnelle), reçu du back-end
 * via `GET api/parametres-globaux` à chaque synchro descendante. `id` = id back (pas d'auto-gen).
 *
 * ⚠️ Room/SQLite ne contraint pas la longueur des colonnes `TEXT` : les tailles du modèle
 * back-end (clef 100, valeur 255, description 500) sont indicatives.
 */
@Entity(
    tableName = "parametre",
    indices = [Index(value = ["clef"], unique = true)]
)
data class ParametreEntity(
    @PrimaryKey
    val id: Long,
    // length 100, non nul, unique
    val clef: String,
    // length 255, non nul
    val valeur: String,
    // length 500, nullable
    val description: String? = null
)
