package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Table outbox locale : les signalements y sont enregistrés puis envoyés via la synchro montante.
// Pas de clés étrangères volontairement : un signalement non transféré ne doit jamais être
// supprimé en cascade par un diff descendant (deleteSitesNotIn, etc.).
@Entity(tableName = "signalement")
data class SignalementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteId: Long,
    val contratId: Long,
    val evenementContratId: Long,   // jointureId de contrat_evenements
    val agentId: Long,
    val dateSignalement: Long,
    val commentaire: String? = null,
    val transactionId: String = java.util.UUID.randomUUID().toString()
)
