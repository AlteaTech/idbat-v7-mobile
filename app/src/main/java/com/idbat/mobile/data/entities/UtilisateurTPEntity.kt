package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utilisateurs_tp")
data class UtilisateurTPEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val login: String,
    val pin: String, // Code PIN à 4 chiffres
    val token: String? = null,
    val lastLoginDate: Long = 0
)
