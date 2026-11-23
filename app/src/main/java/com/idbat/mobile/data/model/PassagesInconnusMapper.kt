package com.idbat.mobile.data.model

import com.idbat.mobile.data.entities.CarteInconnueEntity
import com.idbat.mobile.data.entities.MotifAutorisationContratEntity
import com.idbat.mobile.generated.client.model.CarteInconnueDmo
import com.idbat.mobile.generated.client.model.ContratMotifAutorisationDmo

/**
 * #533 : mapping DMO → entités des passages inconnus, partagé par les deux chemins de synchro
 * descendante (`AuthManager.saveContractToDatabase` et `SyncManager.applyDiff`).
 */
fun List<ContratMotifAutorisationDmo>.toMotifAutorisationEntities(contratId: Long) =
    map { MotifAutorisationContratEntity(id = it.id, contratId = contratId, libelle = it.libelle) }

fun List<CarteInconnueDmo>.toCarteInconnueEntities() =
    map {
        CarteInconnueEntity(
            valeur = it.valeur.uppercase(),
            id = it.id,
            motifAutorisationContratId = it.motifAutorisationContratId,
            motifAutorisationLibelle = it.motifAutorisationLibelle,
            nbPassages = it.nbPassages
        )
    }
        // Clé = immatriculation : en cas de doublon côté back, on garde le compteur le plus haut
        .groupBy { it.valeur }
        .map { (_, doublons) -> doublons.maxBy { it.nbPassages } }
