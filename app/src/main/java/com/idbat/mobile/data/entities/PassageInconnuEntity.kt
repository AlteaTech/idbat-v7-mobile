package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * #533 RG3/RG6 : passage d'une **immatriculation inconnue** (outbox) → `POST api/passages-inconnus`.
 * Contrôle d'accès seul : pas de matières, pas de carte ni d'usager connus. Le back crée/incrémente
 * sa ligne `cartesinconnues` à la réception (immatriculation + motif).
 *
 * Sans FK (comme `passage_refuse`) : une ligne d'outbox ne doit pas disparaître sur un diff de synchro.
 */
@Entity(
    tableName = "passage_inconnu",
    indices = [Index("contratId"), Index("siteId"), Index("userTpId")]
)
data class PassageInconnuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateHeure: Long,
    val contratId: Long,
    val siteId: Long,
    val userTpId: Long,
    val immatriculation: String,
    val motifAutorisationContratId: Long,
    val numeroBonPassage: String,
    val commentaire: String? = null,
    // Accès simple → mode de paiement 0
    val modePaiement: Int = 0,
    val nbUnitesDecomptees: Int = 1,
    val transactionId: String = java.util.UUID.randomUUID().toString(),
    // RG3 (rétention) : horodatage de l'envoi réussi au BO (null = non envoyé)
    val sentAt: Long? = null
)
