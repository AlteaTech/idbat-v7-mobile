package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Passage **refusé** (outbox). Un passage est refusé quand l'écran affiche une alerte bloquante
 * (carte en liste noire, ou seuil atteint) et seulement un bouton « Fermer » (RG1.1).
 *
 * RG2 : stockage identique à un passage, **sans** `numeroBonPassage`, ni solde de points, valeur
 * ou mode de paiement. RG3 : le message d'alerte justifiant le refus est stocké dans `commentaire`.
 *
 * **Sans FK** (comme `signalement`/`carte_creee`) : une ligne d'outbox ne doit jamais être supprimée
 * par un diff de synchro qui retirerait son site/contrat.
 */
@Entity(
    tableName = "passage_refuse",
    indices = [Index("contratId"), Index("siteId"), Index("userTpId")]
)
data class PassageRefuseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateHeure: Long,
    val contratId: Long,
    val siteId: Long,
    val carteId: Long?,
    val usagerId: Long? = null,
    val userTpId: Long,
    // RG3 : message d'alerte affiché justifiant le refus
    val commentaire: String,
    val emailUsager: String? = null,
    val uidCarte: String? = null,
    val transactionId: String = java.util.UUID.randomUUID().toString(),
    // RG3 (rétention) : horodatage de l'envoi réussi au BO (null = non envoyé)
    val sentAt: Long? = null
)
