package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "passage_document",
    foreignKeys = [
        ForeignKey(
            entity = PassageEntity::class,
            parentColumns = ["id"],
            childColumns = ["passageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("passageId")]
)
data class PassageDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val passageId: Long,
    val type: String,        // "photo" | "signature"
    val base64: String,
    val mimeType: String,
    val nomFichier: String
)
