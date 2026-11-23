package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * #533 : immatriculation inconnue (≈ `cartesinconnues` côté back) déjà présentée sur le contrat.
 *
 * Miroir du back (remplacé à chaque synchro descendante) mais tenu à jour localement entre deux
 * synchros : créé avec `nbPassages = 1` au 1er passage (RG4), incrémenté aux suivants (RG7).
 * `nbPassages` est comparé à `contrat.nbPassagesInconnusAcceptes` pour accepter ou refuser.
 *
 * `valeur` (immatriculation) est la clé : `id` back est null tant que la ligne n'a été créée que
 * localement. Sans FK.
 */
@Entity(tableName = "carte_inconnue")
data class CarteInconnueEntity(
    @PrimaryKey
    val valeur: String,
    val id: Long? = null,
    val motifAutorisationContratId: Long,
    val motifAutorisationLibelle: String? = null,
    val nbPassages: Int
)
