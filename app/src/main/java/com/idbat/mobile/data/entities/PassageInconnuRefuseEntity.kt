package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * #533 RG8.3 : passage refusé d'une immatriculation inconnue (nombre de passages exceptionnels
 * autorisés atteint) — outbox → `POST api/passages-inconnus-refuses`. Sans FK.
 */
@Entity(
    tableName = "passage_inconnu_refuse",
    indices = [Index("contratId"), Index("siteId"), Index("userTpId")]
)
data class PassageInconnuRefuseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateHeure: Long,
    val contratId: Long,
    val siteId: Long,
    val userTpId: Long,
    val immatriculation: String,
    val motifAutorisationContratId: Long,
    val commentaire: String? = null,
    val transactionId: String = java.util.UUID.randomUUID().toString(),
    val sentAt: Long? = null
)
