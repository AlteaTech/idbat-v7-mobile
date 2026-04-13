package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contrats")
data class ContratEntity(
    @PrimaryKey
    val id: Long,
    val trigramme: String,
    val nom: String
)
