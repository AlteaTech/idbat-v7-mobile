package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(
    tableName = "contrat_evenements",
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
data class ContratEvenementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @Json(name = "evenementId")
    val evenementId: Long,
    val libelle: String,
    val jointureId: Long,
    val contratId: Long,
    val isEnable: Boolean
)
