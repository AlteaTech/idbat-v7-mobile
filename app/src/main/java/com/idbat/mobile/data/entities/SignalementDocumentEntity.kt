package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signalement_document",
    foreignKeys = [
        ForeignKey(
            entity = SignalementEntity::class,
            parentColumns = ["id"],
            childColumns = ["signalementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("signalementId")]
)
data class SignalementDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val signalementId: Long,
    val base64: String,
    val mimeType: String,
    val nomFichier: String
)
