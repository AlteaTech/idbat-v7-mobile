package com.idbat.mobile.data.entities

import androidx.room.*

@Entity(
    tableName = "sites",
    foreignKeys = [
        ForeignKey(
            entity = ContratEntity::class,
            parentColumns = ["id"],
            childColumns = ["contratId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contratId")] // Un index sur la clé étrangère est recommandé par Room
)
data class SiteEntity(
    @PrimaryKey
    val id: Long,
    val trigramme: String,
    val nom: String,
    val adresse1: String?,
    val adresse2: String?,
    val codePostal: String?,
    val ville: String?,
    val typeImprimante: String?,
    val macImprimante: String?,
    val horairesOuverture: String?,
    val destinatairesMailTransfertTP: String?,
    val contratId: Long // Clé étrangère vers contrats.id
) {
    // Room ne peut pas stocker directement une liste d'entités dans une table.
    // L'annotation @Ignore indique à Room d'ignorer ce champ lors de la création de la table.
    // Pour récupérer un site avec ses matières, il faudra créer une classe de relation (ex: SiteAvecMatieres).
    @Ignore
    var matieres: List<MatiereSiteEntity> = emptyList()
}
