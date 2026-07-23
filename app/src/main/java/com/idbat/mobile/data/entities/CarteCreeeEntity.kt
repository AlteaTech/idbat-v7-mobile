package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Journal local des cartes à puce créées avec succès (écriture NFC + relecture conformes).
 * Pas de FK : un enregistrement de création ne doit jamais être supprimé par un diff de synchro.
 */
@Entity(tableName = "carte_creee")
data class CarteCreeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contratId: Long,
    val siteId: Long,                 // site connecté au moment de la création
    val userTpId: Long,               // utilisateur TP connecté au moment de la création
    val type: String,                 // 'P' (carte à puce)
    val succes: Boolean = true,
    val uid: String,
    val numeroSerie: String,
    val numeroIdentification: String,
    val motDePasse: String,
    val societe: String,
    val interne: Boolean,
    val prepaiement: Boolean,
    val facturation: Boolean,
    val gratuit: Boolean,
    val nomPrenom: String,
    val identClient: String,
    val soldePoints: String,
    val cumulPoints: String,
    val dateCreation: Long,
    // RG3 : horodatage de l'envoi réussi au BO (null = non envoyé). Sert à différer la purge.
    val sentAt: Long? = null
)
