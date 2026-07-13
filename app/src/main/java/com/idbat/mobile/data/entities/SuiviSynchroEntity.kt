package com.idbat.mobile.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Suivi (audit) d'une exécution de synchronisation. Une ligne est créée au **début** avec
 * `idTransaction`, `sens`, `siteId`, `nbAEnvoyer` et `dateDebut` ; `nbEnvoye` et `dateFin` sont
 * renseignés à la **fin**.
 *
 * `siteId` **sans FK** (comme `signalement`/`carte_creee`) : une ligne de suivi ne doit jamais être
 * supprimée par un diff de synchro qui retirerait le site.
 */
@Entity(
    tableName = "suivi_synchro",
    indices = [
        Index(value = ["idTransaction"], unique = true),
        Index("siteId")
    ]
)
data class SuiviSynchroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // GUID de la transaction de synchro
    val idTransaction: String,
    // Sens de la synchro (montant / descendant)
    val sens: SensSynchro,
    val siteId: Long,
    // Utilisateur TP connecté au moment de la synchro
    val utilisateurTpId: Long?,
    // Nombre d'opérations à envoyer (connu au démarrage)
    val nbAEnvoyer: Int,
    // Nombre effectivement envoyé (null tant que la synchro n'est pas terminée)
    val nbEnvoye: Int? = null,
    val dateDebut: Date,
    val dateFin: Date? = null,
    // Horodatages d'envoi de la ligne de suivi vers le back (null = non envoyé)
    val sentAt1: Date? = null,
    val sentAt2: Date? = null
)
