package com.idbat.mobile.singleton

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.*
import com.idbat.mobile.generated.client.api.ContratsControllerApi
import com.idbat.mobile.generated.client.model.ContratDmo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val database: AppDatabase,
    private val tokenStore: TokenStore,
    private val contratsApi: ContratsControllerApi
) {
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState

    data class SyncState(
        val lastSynchroDateEnvoi: Date? = null,
        val lastSynchroDateReception: Date? = null,
        val isTransferring: Boolean = false,
        val syncError: String? = null
    )

    suspend fun loadSyncDatesForSite(site: SiteEntity) {
        try {
            val dao = database.lastSynchroHistoryDao()
            val lastEnvoi = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.ENVOI)
            val lastReception = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.RECEPTION)

            _syncState.value = _syncState.value.copy(
                lastSynchroDateEnvoi = lastEnvoi?.date,
                lastSynchroDateReception = lastReception?.date
            )
        } catch (e: Exception) {
            Log.e("SYNC_MANAGER", "Erreur lors du chargement des dates", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun executeTransfer(site: SiteEntity) {
        _syncState.value = _syncState.value.copy(isTransferring = true)
        try {
            Log.d("SYNC_MANAGER", "Début du transfert pour le site ${site.nom} — token: ${tokenStore.token}")

            if (!ConfigSingleton.webEnable) {
                Log.d("SYNC_MANAGER", "Mode hors ligne — synchronisation ignorée")
                _syncState.value = _syncState.value.copy(isTransferring = false)
                return
            }

            // 1. Bloquer si des passages non transférés existent
            val passageCount = database.passageDao().count()
            if (passageCount > 0) {
                _syncState.value = _syncState.value.copy(
                    isTransferring = false,
                    syncError = "Impossible de synchroniser : $passageCount passage(s) en attente de transfert.\nVeuillez d'abord envoyer les passages."
                )
                return
            }

            // 2. Récupération depuis l'API
            val response = contratsApi.getByDevice()
            if (!response.isSuccessful) {
                _syncState.value = _syncState.value.copy(
                    isTransferring = false,
                    syncError = "Erreur serveur lors de la synchronisation (code ${response.code()})"
                )
                return
            }
            val dmo = response.body() ?: run {
                _syncState.value = _syncState.value.copy(
                    isTransferring = false,
                    syncError = "Le serveur n'a renvoyé aucune donnée."
                )
                return
            }

            // 3. Diff en base de données
            withContext(Dispatchers.IO) { applyDiff(dmo) }

            // 4. Comptage total des lignes récupérées (toutes tables confondues)
            val totalRows = countDmoRows(dmo)

            // 5. Mise à jour de l'historique de synchro pour le site courant
            val dateExec = Date()
            val histoDao = database.lastSynchroHistoryDao()
            histoDao.deleteTypeForSite(site.id, TypeSynchro.RECEPTION)
            histoDao.insertSynchro(
                LastSynchroHistoryEntity(
                    siteId = site.id,
                    date = dateExec,
                    type = TypeSynchro.RECEPTION,
                    operationsTentees = totalRows,
                    operationsReussies = totalRows
                )
            )

            _syncState.value = _syncState.value.copy(
                lastSynchroDateReception = dateExec,
                isTransferring = false
            )
            Log.d("SYNC_MANAGER", "Synchronisation terminée — $totalRows lignes récupérées")

        } catch (e: Exception) {
            Log.e("SYNC_MANAGER", "Erreur lors du transfert", e)
            _syncState.value = _syncState.value.copy(
                isTransferring = false,
                syncError = "Erreur inattendue : ${e.message}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun applyDiff(dmo: ContratDmo) {
        val contratDao          = database.contratDao()
        val siteDao             = database.siteDao()
        val matiereSiteDao      = database.matiereSiteDao()
        val utilisateurTPDao    = database.utilisateurTPDao()
        val usagerDao           = database.usagerDao()
        val carteContratDao     = database.carteContratDao()
        val usagerCarteDao      = database.usagerCarteDao()
        val contratEvenementDao = database.contratEvenementDao()

        // ── UPSERT (insert or replace) ───────────────────────────────────────────

        val contratEntity = ContratEntity(
            id = dmo.id,
            trigramme = dmo.trigramme,
            nom = dmo.nom,
            hasPuce = dmo.hasPuce,
            hasCodebarres = dmo.hasCodebarres,
            hasImmatriculation = dmo.hasImmatriculation,
            hasSelectionusager = dmo.hasSelectionusager,
            hasSignatureParticuliers = dmo.hasSignatureparticuliers,
            hasSignatureProfessionnels = dmo.hasSignatureprofessionels
        )
        contratDao.insertContrat(contratEntity)

        val siteEntities = dmo.contratSite.map { siteDmo ->
            SiteEntity(
                id = siteDmo.id,
                trigramme = siteDmo.trigramme,
                nom = siteDmo.nom,
                adresse1 = siteDmo.adresse1,
                adresse2 = siteDmo.adresse2,
                codePostal = siteDmo.codePostal,
                ville = siteDmo.ville,
                typeImprimante = siteDmo.typeImprimante,
                macImprimante = siteDmo.macImprimante,
                horairesOuverture = siteDmo.horairesOuverture,
                destinatairesMailTransfertTP = siteDmo.destinatairesMailTransfertTP,
                contratId = contratEntity.id
            )
        }
        if (siteEntities.isNotEmpty()) siteDao.insertSites(siteEntities)

        val utilisateurEntities = dmo.contratUtilisateursTps.map { utpDmo ->
            UtilisateurTPEntity(
                id = utpDmo.id ?: 0,
                login = utpDmo.login,
                pin = utpDmo.motDePasse
            )
        }
        if (utilisateurEntities.isNotEmpty()) utilisateurTPDao.insertUtilisateurs(utilisateurEntities)

        val usagerEntities = dmo.contratUsagers.map { usagerDmo ->
            UsagerEntity(
                id = usagerDmo.id,
                nom = usagerDmo.nom,
                prenom = usagerDmo.prenom,
                refClientIdBat = usagerDmo.refClientIdBat,
                contratId = usagerDmo.contratId,
                raisonSociale = usagerDmo.raisonSociale,
                typeApporteurLibelle = usagerDmo.typeApporteurLibelle,
                couriel = usagerDmo.couriel,
                typeApporteurIsPro = usagerDmo.typeApporteurIsPro,
            )
        }
        coroutineScope {
            usagerEntities.chunked(2000).map { lot -> async { usagerDao.insertUsagers(lot) } }.awaitAll()
        }

        val allCartes = dmo.contratUsagers.flatMap { usagerDmo ->
            usagerDmo.cartes.map { carteDmo ->
                CarteContratEntity(
                    id = carteDmo.id,
                    libelle = "",
                    type = carteDmo.type,
                    valeur = carteDmo.valeur.ifBlank { null },
                    uidRfid = carteDmo.uidRfid,
                    isCreationByQRCode = carteDmo.isCreationByQRCode,
                    carteGriseJ1 = carteDmo.carteGriseJ1,
                    carteGriseF3 = carteDmo.carteGriseF3,
                    contratId = contratEntity.id,
                    carteId = carteDmo.id
                )
            }
        }
        if (allCartes.isNotEmpty()) carteContratDao.insertCartes(allCartes)

        // ── TABLES DE LIAISON : clear + reinsert ────────────────────────────────

        matiereSiteDao.purge()
        val allMatieres = dmo.contratSite.flatMap { siteDmo ->
            siteDmo.matieres.map { matiereDmo ->
                MatiereSiteEntity(
                    siteId = siteDmo.id,
                    matiereId = matiereDmo.id,
                    libelle = matiereDmo.libelle,
                    unitesDesApportId = matiereDmo.unitesDesApportId,
                    unitesDesApportLibelle = matiereDmo.unitesDesApportLibelle,
                    tarif = matiereDmo.tarif.toDouble()
                )
            }
        }
        if (allMatieres.isNotEmpty()) matiereSiteDao.insertMatieres(allMatieres)

        usagerCarteDao.clearUsagerCartes()
        val allUsagerCartes = dmo.contratUsagers.flatMap { usagerDmo ->
            usagerDmo.cartes.map { carteDmo ->
                UsagerCarteEntity(
                    usagerId = usagerDmo.id,
                    carteId = carteDmo.id,
                    dateDebut = java.util.Date.from(
                        carteDmo.dateDebutAffectation
                            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                    ),
                    dateFin = carteDmo.dateFinAffectation?.let {
                        java.util.Date.from(
                            it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                        )
                    }
                )
            }
        }
        if (allUsagerCartes.isNotEmpty()) usagerCarteDao.insertUsagerCartes(allUsagerCartes)

        contratEvenementDao.clearEvenements()
        val allEvenements = dmo.evenementsContrat.map { evDmo ->
            ContratEvenementEntity(
                evenementId = evDmo.evenementId,
                libelle = evDmo.libelle,
                jointureId = evDmo.jointureId,
                contratId = contratEntity.id
            )
        }
        coroutineScope {
            allEvenements.chunked(500).map { lot -> async { contratEvenementDao.insertEvenements(lot) } }.awaitAll()
        }

        // ── SUPPRESSION DES LIGNES OBSOLÈTES ────────────────────────────────────

        val newCarteIds = allCartes.map { it.id }
        if (newCarteIds.isNotEmpty()) carteContratDao.deleteCartesNotIn(newCarteIds)
        else carteContratDao.clearCartes()

        val newUsagerIds = usagerEntities.map { it.id }
        if (newUsagerIds.isNotEmpty()) usagerDao.deleteUsagersNotIn(newUsagerIds)
        else usagerDao.purge()

        val newSiteIds = siteEntities.map { it.id }
        if (newSiteIds.isNotEmpty()) siteDao.deleteSitesNotIn(newSiteIds)
        else siteDao.purge()

        val newUtpIds = utilisateurEntities.mapNotNull { if (it.id != 0L) it.id else null }
        if (newUtpIds.isNotEmpty()) utilisateurTPDao.deleteUtilisateursNotIn(newUtpIds)
        else utilisateurTPDao.clearUtilisateursExcludingAdmin()

        Log.d("SYNC_MANAGER", "Diff appliqué : ${siteEntities.size} sites, ${usagerEntities.size} usagers, ${allCartes.size} cartes")
    }

    private fun countDmoRows(dmo: ContratDmo): Long {
        val sites        = dmo.contratSite.size.toLong()
        val utilisateurs = dmo.contratUtilisateursTps.size.toLong()
        val usagers      = dmo.contratUsagers.size.toLong()
        val cartes       = dmo.contratUsagers.sumOf { it.cartes.size }.toLong()
        val matieres     = dmo.contratSite.sumOf { it.matieres.size }.toLong()
        val usagerCartes = cartes
        val evenements   = dmo.evenementsContrat.size.toLong()
        return 1L + sites + utilisateurs + usagers + cartes + matieres + usagerCartes + evenements
    }

    fun clearSyncError() {
        _syncState.value = _syncState.value.copy(syncError = null)
    }

    fun clearSyncData() {
        _syncState.value = SyncState()
    }
}
