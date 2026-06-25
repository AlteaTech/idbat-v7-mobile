package com.idbat.mobile.singleton

import android.content.Context
import android.database.AbstractWindowedCursor
import android.database.CursorWindow
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.*
import com.idbat.mobile.generated.client.api.CarteCreationControllerApi
import com.idbat.mobile.generated.client.api.ContratsControllerApi
import com.idbat.mobile.generated.client.api.PassagesControllerApi
import com.idbat.mobile.generated.client.api.SignalementsControllerApi
import com.idbat.mobile.generated.client.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val tokenStore: TokenStore,
    private val contratsApi: ContratsControllerApi,
    private val passagesApi: PassagesControllerApi,
    private val signalementsApi: SignalementsControllerApi,
    private val carteCreationApi: CarteCreationControllerApi
) {
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState

    data class SyncState(
        val lastSynchroDateEnvoi: Date? = null,
        val lastSynchroDateReception: Date? = null,
        val isTransferring: Boolean = false,
        val syncError: String? = null,
        val lastEnvoiSuccess: Boolean? = null  // null=jamais, true=tout OK, false=erreurs
    )

    suspend fun loadSyncDatesForSite(site: SiteEntity) {
        try {
            val dao = database.lastSynchroHistoryDao()
            val lastEnvoi = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.ENVOI)
            val lastReception = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.RECEPTION)
            _syncState.value = _syncState.value.copy(
                lastSynchroDateEnvoi = lastEnvoi?.date,
                lastSynchroDateReception = lastReception?.date,
                lastEnvoiSuccess = lastEnvoi?.let { it.operationsReussies == it.operationsTentees }
            )
        } catch (e: Exception) {
            Log.e("SYNC_MANAGER", "Erreur lors du chargement des dates", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun executeTransfer(site: SiteEntity) {
        _syncState.value = _syncState.value.copy(isTransferring = true)

        // Maintenir CPU + WiFi actifs pendant tout le transfert : sans ça, écran éteint =
        // light doze → réseau coupé → tous les appels échouent.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "idbat:sync")
        val wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "idbat:sync")

        try {
            wakeLock.acquire(2 *60 * 60 * 1000L /* timeout sécurité 2h */)
            wifiLock.acquire()

            synchroMontante(site)
            synchroDescendante(site)

            // RG3 : à chaque synchro, purge des données envoyées et saisies il y a plus de X jours
            purgeOldSyncedData()
        } catch (e: Exception) {
            Log.e("SYNC_MANAGER", "Erreur critique lors du transfert", e)
            _syncState.value = _syncState.value.copy(syncError = "Erreur inattendue : ${e.message}")
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
            if (wifiLock.isHeld) wifiLock.release()
            _syncState.value = _syncState.value.copy(isTransferring = false)
        }
    }

    /**
     * RG3 : supprime les données saisies (passages, signalements, cartes créées) qui ont été
     * envoyées avec succès et dont l'horodatage de saisie dépasse la durée de rétention
     * (ConfigSingleton.dataRetentionDays). Les lignes non envoyées ne sont jamais touchées.
     */
    private suspend fun purgeOldSyncedData() {
        val threshold = System.currentTimeMillis() -
                ConfigSingleton.dataRetentionDays * 24L * 60L * 60L * 1000L
        database.passageDao().deleteSentOlderThan(threshold)
        database.signalementDao().deleteSentOlderThan(threshold)
        database.carteCreeeDao().deleteSentOlderThan(threshold)
        Log.d("SYNC_MANAGER", "Purge RG3 effectuée (seuil = $threshold)")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun synchroMontante(site: SiteEntity) {
        val passageDao      = database.passageDao()
        val matiereDao      = database.passageMatiereDao()
        val usagerDao       = database.usagerDao()
        val signalementDao  = database.signalementDao()
        val carteCreeeDao   = database.carteCreeeDao()
        val histoDao        = database.lastSynchroHistoryDao()

        // RG3 : on n'envoie que ce qui n'a pas encore été transmis avec succès.
        // Les lignes déjà envoyées restent en base et seront purgées après X jours.
        val nowMillis       = System.currentTimeMillis()
        val allPassages     = passageDao.getUnsentPassages()
        val allSignalements = signalementDao.getUnsent()
        val allCartesCreees = carteCreeeDao.getUnsent()
        Log.d("SYNC_MANAGER", "Synchro montante : ${allPassages.size} passage(s) + ${allSignalements.size} signalement(s) + ${allCartesCreees.size} carte(s) créée(s) à envoyer")

        // Note : même s'il n'y a rien à envoyer, on continue. Un contrôle « rien à envoyer »
        // compte comme un envoi réussi et doit rafraîchir l'horodatage (cf. statsBySite plus bas).

        // Stats par siteId : Pair(tentées, réussies)
        val statsBySite = mutableMapOf<Long, Pair<Long, Long>>()

        for (passage in allPassages) {
            val matieres  = matiereDao.getMatieresByPassage(passage.id)
            val documents = getDocumentsForSync(passage.id)
            val usagerId  = passage.carteId?.let { usagerDao.getUsagerByCarte(it)?.id }

            val request = CreerPassageRequest(
                contratId        = passage.contratId,
                siteId           = passage.siteId,
                userTpId         = passage.userTpId,
                transactionId    = passage.transactionId,
                datePassage      = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(passage.dateHeure), ZoneId.systemDefault()
                ),
                numeroBonPassage = passage.numeroBonPassage,
                matieres         = matieres.map { m ->
                    PassageMatiereRequest(
                        matieresSiteId = m.matiereId,
                        quantite       = BigDecimal(m.quantite)
                    )
                },
                documents        = documents.map { d ->
                    PassageDocumentRequest(
                        type      = d.type,
                        nomFichier = d.nomFichier,
                        mimeType  = d.mimeType,
                        base64    = Base64.decode(d.base64, Base64.DEFAULT)
                    )
                },
                usagerId    = usagerId,
                carteId     = passage.carteId,
                commentaire = passage.commentaire,
                emailUsager = passage.emailUsager
            )

            val prev = statsBySite.getOrDefault(passage.siteId, 0L to 0L)

            try {
                val response = passagesApi.creer(request)
                if (response.isSuccessful) {
                    passageDao.markSent(passage.id, nowMillis)
                    statsBySite[passage.siteId] = (prev.first + 1) to (prev.second + 1)
                    Log.d("SYNC_MANAGER", "Passage ${passage.id} envoyé (site ${passage.siteId})")
                } else {
                    statsBySite[passage.siteId] = (prev.first + 1) to prev.second
                    Log.w("SYNC_MANAGER", "Passage ${passage.id} refusé — code ${response.code()}")
                }
            } catch (e: Exception) {
                statsBySite[passage.siteId] = (prev.first + 1) to prev.second
                Log.e("SYNC_MANAGER", "Erreur envoi passage ${passage.id}", e)
            }
        }

        // ── Signalements ────────────────────────────────────────────────────────
        for (signalement in allSignalements) {
            val documents = getSignalementDocumentsForSync(signalement.id)

            val request = CreerSignalementRequest(
                siteId             = signalement.siteId,
                evenementContratId = signalement.evenementContratId,
                agentId            = signalement.agentId,
                dateSignalement    = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(signalement.dateSignalement), ZoneId.systemDefault()
                ),
                transactionId      = signalement.transactionId,
                commentaire        = signalement.commentaire,
                photos             = documents.map { d ->
                    FileData(
                        fileName = d.nomFichier,
                        mimeType = d.mimeType,
                        base64   = Base64.decode(d.base64, Base64.DEFAULT)
                    )
                }.ifEmpty { null }
            )

            val prev = statsBySite.getOrDefault(signalement.siteId, 0L to 0L)
            try {
                val response = signalementsApi.creerSignalement(request)
                if (response.isSuccessful) {
                    signalementDao.markSent(signalement.id, nowMillis)
                    statsBySite[signalement.siteId] = (prev.first + 1) to (prev.second + 1)
                    Log.d("SYNC_MANAGER", "Signalement ${signalement.id} envoyé (site ${signalement.siteId})")
                } else {
                    statsBySite[signalement.siteId] = (prev.first + 1) to prev.second
                    Log.w("SYNC_MANAGER", "Signalement ${signalement.id} refusé — code ${response.code()}")
                }
            } catch (e: Exception) {
                statsBySite[signalement.siteId] = (prev.first + 1) to prev.second
                Log.e("SYNC_MANAGER", "Erreur envoi signalement ${signalement.id}", e)
            }
        }

        // ── Cartes créées ───────────────────────────────────────────────────────
        for (carte in allCartesCreees) {
            val request = MarquerCarteQrCodeRequest(
                uid                   = carte.uid,
                numeroIdentification  = carte.numeroIdentification,
                userTpId              = carte.userTpId,
                dateCreationMobile    = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(carte.dateCreation), ZoneId.systemDefault()
                )
            )

            val prev = statsBySite.getOrDefault(carte.siteId, 0L to 0L)
            try {
                val response = carteCreationApi.marquerCarteCreationParQrCode(request)
                if (response.isSuccessful) {
                    carteCreeeDao.markSent(carte.id, nowMillis)
                    Log.d("SYNC_MANAGER", "Carte créée ${carte.id} envoyée (uid ${carte.uid})")
                    statsBySite[carte.siteId] = (prev.first + 1) to (prev.second + 1)

                } else {
                    statsBySite[carte.siteId] = (prev.first + 1) to prev.second
                    Log.w("SYNC_MANAGER", "Carte créée ${carte.id} refusée — code ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SYNC_MANAGER", "Erreur envoi carte créée ${carte.id}", e)
            }
        }

        // Historique ENVOI par site ayant réellement eu des opérations
        val dateExec = Date()
        for ((siteId, stats) in statsBySite) {
            histoDao.deleteTypeForSite(siteId, TypeSynchro.ENVOI)
            histoDao.insertSynchro(
                LastSynchroHistoryEntity(
                    siteId             = siteId,
                    date               = dateExec,
                    type               = TypeSynchro.ENVOI,
                    operationsTentees  = stats.first,
                    operationsReussies = stats.second
                )
            )
        }

        // « Rien à envoyer pour le site courant » compte comme un contrôle d'envoi réussi :
        // on rafraîchit l'horodatage ENVOI en conservant le compteur d'opérations précédent.
        if (!statsBySite.containsKey(site.id)) {
            val previous = histoDao.getLastSynchroForSiteAndType(site.id, TypeSynchro.ENVOI)
            histoDao.deleteTypeForSite(site.id, TypeSynchro.ENVOI)
            histoDao.insertSynchro(
                LastSynchroHistoryEntity(
                    siteId             = site.id,
                    date               = dateExec,
                    type               = TypeSynchro.ENVOI,
                    operationsTentees  = previous?.operationsTentees ?: 0L,
                    operationsReussies = previous?.operationsReussies ?: 0L
                )
            )
        }

        // Rafraîchir le state pour le site courant (envoi réussi OU rien à envoyer)
        val currentStats = statsBySite[site.id]
        _syncState.value = _syncState.value.copy(
            lastSynchroDateEnvoi = dateExec,
            lastEnvoiSuccess = currentStats?.let { it.second == it.first } ?: true
        )

        Log.d("SYNC_MANAGER", "Synchro montante terminée — stats: $statsBySite")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun synchroDescendante(site: SiteEntity) {
        // Bloquer si des passages non transférés existent après la montante
        // RG3 : on ne bloque que sur les lignes encore à envoyer (les lignes déjà
        // transmises restent en base le temps de la rétention, mais n'empêchent pas la réception).
        val passageCount = database.passageDao().countUnsent()
        val signalementCount = database.signalementDao().countUnsent()
        if (passageCount > 0) {
            _syncState.value = _syncState.value.copy(
                syncError = "Impossible de synchroniser : $passageCount passage(s) en attente de transfert.\nVeuillez d'abord envoyer les passages."
            )
            return
        }
        if (signalementCount > 0) {
            _syncState.value = _syncState.value.copy(
                syncError = "Impossible de synchroniser : $signalementCount signalement(s) en attente de transfert.\nVeuillez d'abord envoyer les signalements."
            )
            return
        }

        val response = contratsApi.getByDevice()
        if (!response.isSuccessful) {
            _syncState.value = _syncState.value.copy(
                syncError = "Erreur serveur lors de la synchronisation (code ${response.code()})"
            )
            return
        }
        val dmo = response.body() ?: run {
            _syncState.value = _syncState.value.copy(syncError = "Le serveur n'a renvoyé aucune donnée.")
            return
        }

        // Transaction : tout le diff est atomique (rien n'est écrit si une étape échoue)
        database.withTransaction { applyDiff(dmo) }

        val totalRows = countDmoRows(dmo)
        val dateExec  = Date()
        val histoDao  = database.lastSynchroHistoryDao()

        // Les données descendantes sont communes à tous les sites — on met à jour l'historique pour chacun
        dmo.contratSite.forEach { siteDmo ->
            histoDao.deleteTypeForSite(siteDmo.id, TypeSynchro.RECEPTION)
            histoDao.insertSynchro(
                LastSynchroHistoryEntity(
                    siteId             = siteDmo.id,
                    date               = dateExec,
                    type               = TypeSynchro.RECEPTION,
                    operationsTentees  = totalRows,
                    operationsReussies = totalRows
                )
            )
        }

        _syncState.value = _syncState.value.copy(lastSynchroDateReception = dateExec)
        Log.d("SYNC_MANAGER", "Synchro descendante terminée — $totalRows lignes récupérées (${dmo.contratSite.size} sites mis à jour)")
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
        val seuilEtatDao        = database.seuilEtatDao()

        // ── UPSERT ──────────────────────────────────────────────────────────────

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
            UtilisateurTPEntity(id = utpDmo.id ?: 0, login = utpDmo.login, pin = utpDmo.motDePasse)
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
        // Séquentiel : on est dans une transaction Room (mono-thread), pas de parallélisme DB
        usagerEntities.chunked(2000).forEach { lot -> usagerDao.insertUsagers(lot) }

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
                    carteId = carteDmo.id,
                    isEnListeNoire = carteDmo.isEnListeNoire,
                    dtEntreeListeNoire = carteDmo.dtEntreeListeNoire?.let {
                        Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    },
                    dtSortieListeNoire = carteDmo.dtSortieListeNoire?.let {
                        Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    },
                    motifListeNoireContratId = carteDmo.motifslistenoireContratId,
                    motifListeNoireLibelle = carteDmo.motifslistenoireLibelle
                )
            }
        }
        if (allCartes.isNotEmpty()) carteContratDao.insertCartes(allCartes)

        // ── TABLES DE LIAISON : clear + reinsert ────────────────────────────────

        matiereSiteDao.purge()
        val allMatieres = dmo.contratSite.flatMap { siteDmo ->
            siteDmo.matieres.map { m ->
                MatiereSiteEntity(
                    siteId = siteDmo.id,
                    matiereId = m.id,
                    libelle = m.libelle,
                    isEnable= m.isEnable,
                    unitesDesApportId = m.unitesDesApportId,
                    unitesDesApportLibelle = m.unitesDesApportLibelle,
                    tarif = m.tarif.toDouble()
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
                    dateDebut = Date.from(
                        carteDmo.dateDebutAffectation.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    ),
                    dateFin = carteDmo.dateFinAffectation?.let {
                        Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    }
                )
            }
        }
        if (allUsagerCartes.isNotEmpty()) usagerCarteDao.insertUsagerCartes(allUsagerCartes)

        seuilEtatDao.clearSeuils()
        val allSeuils = dmo.contratUsagers.flatMap { usagerDmo ->
            usagerDmo.seuils.map { seuilDmo ->
                SeuilEtatEntity(
                    usagerId = usagerDmo.id,
                    seuilId = seuilDmo.seuilId,
                    nom = seuilDmo.nom,
                    nbPassagesAutorises = seuilDmo.nbPassagesAutorises,
                    nbPassagesEffectues = seuilDmo.nbPassagesEffectues,
                    isAlerte = seuilDmo.isAlerte,
                    seuilDetailNom = seuilDmo.seuilDetailNom,
                    seuilDetailType = seuilDmo.seuilDetailType,
                    seuilDetailPeriode = seuilDmo.seuilDetailPeriode,
                    seuilDetailNbPassage = seuilDmo.seuilDetailNbPassage,
                    seuilDetailSeuilPrevention = seuilDmo.seuilDetailSeuilPrevention,
                    seuilDetailContratId = seuilDmo.seuilDetailContratId
                )
            }
        }
        if (allSeuils.isNotEmpty()) seuilEtatDao.insertSeuils(allSeuils)

        contratEvenementDao.clearEvenements()
        val allEvenements = dmo.evenementsContrat.map { evDmo ->
            ContratEvenementEntity(
                evenementId = evDmo.evenementId,
                libelle = evDmo.libelle,
                jointureId = evDmo.jointureId,
                isEnable = evDmo.isEnable,
                contratId = contratEntity.id
            )
        }
        allEvenements.chunked(500).forEach { lot -> contratEvenementDao.insertEvenements(lot) }

        // ── SUPPRESSION DES LIGNES OBSOLÈTES ────────────────────────────────────

        val newCarteIds = allCartes.map { it.id }
        if (newCarteIds.isNotEmpty()) carteContratDao.deleteCartesNotIn(newCarteIds) else carteContratDao.clearCartes()

        val newUsagerIds = usagerEntities.map { it.id }
        if (newUsagerIds.isNotEmpty()) usagerDao.deleteUsagersNotIn(newUsagerIds) else usagerDao.purge()

        val newSiteIds = siteEntities.map { it.id }
        if (newSiteIds.isNotEmpty()) siteDao.deleteSitesNotIn(newSiteIds) else siteDao.purge()

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
        val seuils       = dmo.contratUsagers.sumOf { it.seuils.size }.toLong()
        return 1L + sites + utilisateurs + usagers + cartes + matieres + usagerCartes + evenements + seuils
    }

    private suspend fun getDocumentsForSync(passageId: Long): List<PassageDocumentEntity> =
        withContext(Dispatchers.IO) {
            val cursor = database.openHelper.readableDatabase.query(
                SimpleSQLiteQuery(
                    "SELECT id, passageId, type, base64, mimeType, nomFichier FROM passage_document WHERE passageId = ?",
                    arrayOf(passageId)
                )
            )
            // CursorWindow par défaut = 2MB — insuffisant pour des photos base64.
            // On le remplace par un de 10MB avant la première lecture.
            if (cursor is AbstractWindowedCursor) {
                cursor.window = CursorWindow("sync_docs", 10L * 1024 * 1024)
            }
            val docs = mutableListOf<PassageDocumentEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    docs.add(
                        PassageDocumentEntity(
                            id        = c.getLong(0),
                            passageId = c.getLong(1),
                            type      = c.getString(2),
                            base64    = c.getString(3),
                            mimeType  = c.getString(4),
                            nomFichier = c.getString(5)
                        )
                    )
                }
            }
            docs
        }

    private suspend fun getSignalementDocumentsForSync(signalementId: Long): List<SignalementDocumentEntity> =
        withContext(Dispatchers.IO) {
            val cursor = database.openHelper.readableDatabase.query(
                SimpleSQLiteQuery(
                    "SELECT id, signalementId, base64, mimeType, nomFichier FROM signalement_document WHERE signalementId = ?",
                    arrayOf(signalementId)
                )
            )
            // CursorWindow par défaut = 2MB — insuffisant pour des photos base64.
            if (cursor is AbstractWindowedCursor) {
                cursor.window = CursorWindow("sync_signalement_docs", 10L * 1024 * 1024)
            }
            val docs = mutableListOf<SignalementDocumentEntity>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    docs.add(
                        SignalementDocumentEntity(
                            id            = c.getLong(0),
                            signalementId = c.getLong(1),
                            base64        = c.getString(2),
                            mimeType      = c.getString(3),
                            nomFichier    = c.getString(4)
                        )
                    )
                }
            }
            docs
        }

    fun clearSyncError() {
        _syncState.value = _syncState.value.copy(syncError = null)
    }

    fun clearSyncData() {
        _syncState.value = SyncState()
    }
}
