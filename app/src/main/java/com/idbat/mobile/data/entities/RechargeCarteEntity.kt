package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Journal local des rechargements de carte à puce (outbox).
 * Enregistré seulement après écriture NFC réussie (RG3.2). Pas de FK : un rechargement
 * ne doit jamais être supprimé par un diff de synchro.
 */
@Entity(tableName = "recharge_carte")
data class RechargeCarteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contratId: Long,
    val siteId: Long,
    val uid: String,
    val numeroIdentification: String,
    val identClient: String,
    // Usager associé à la carte au moment du rechargement (null si non trouvé)
    val usagerId: Long? = null,
    val ancienSolde: Double,
    val pointsRecharges: Double,
    val nouveauSolde: Double,
    val dateRecharge: Long,
    val transactionId: String = java.util.UUID.randomUUID().toString(),
    // pour une future synchro montante (null = non envoyé)
    val sentAt: Long? = null
)
