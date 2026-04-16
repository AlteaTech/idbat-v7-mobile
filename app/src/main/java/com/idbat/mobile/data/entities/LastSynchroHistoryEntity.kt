package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

enum class TypeSynchro {
    ENVOI,
    RECEPTION
}

@Entity(
    tableName = "last_synchro_history_sites",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("siteId")]
)
data class LastSynchroHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteId: Long,
    var date: Date,
    val type: TypeSynchro,
    val operationsTentees: Long = 0, // ← NOUVELLE PROPRIÉTÉ
    val operationsReussies: Long = 0  // ← NOUVELLE PROPRIÉTÉ
)