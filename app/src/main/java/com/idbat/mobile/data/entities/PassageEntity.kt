package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "passage",
    foreignKeys = [
        ForeignKey(
            entity = ContratEntity::class,
            parentColumns = ["id"],
            childColumns = ["contratId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UtilisateurTPEntity::class,
            parentColumns = ["id"],
            childColumns = ["userTpId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contratId"), Index("siteId"), Index("userTpId")]
)
data class PassageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateHeure: Long,
    val contratId: Long,
    val siteId: Long,
    val carteId: Long?,
    val userTpId: Long,
    val numeroBonPassage: String
)
