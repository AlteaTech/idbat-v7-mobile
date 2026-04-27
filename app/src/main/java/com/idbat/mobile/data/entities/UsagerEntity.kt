package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usagers",
    foreignKeys = [
        ForeignKey(
            entity = ContratEntity::class,
            parentColumns = ["id"],
            childColumns = ["contratId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contratId")]
)
data class UsagerEntity(
    @PrimaryKey
    val id: Long,
    val nom: String,
    val prenom: String,
    val contratId: Long,
    val refClientIdBat: Long? = null
)
