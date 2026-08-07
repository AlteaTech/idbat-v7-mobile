package com.idbat.mobile.singleton

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.*
import com.idbat.mobile.generated.client.api.AuthMobileControllerApi
import com.idbat.mobile.generated.client.api.ContratsControllerApi
import com.idbat.mobile.generated.client.api.SmartphonesMobileControllerApi
import com.idbat.mobile.generated.client.model.CreerSmartphoneMobileRequest
import com.idbat.mobile.generated.client.model.LoginMobileRequest
import com.idbat.mobile.generated.client.model.UpdateSmartphoneLocationRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val database: AppDatabase,
    private val authApi: AuthMobileControllerApi,
    private val contratsApi: ContratsControllerApi,
    private val smartphonesApi: SmartphonesMobileControllerApi,
    private val tokenStore: TokenStore,
    @ApplicationContext private val context: Context,
    val syncManager: SyncManager,
    private val parametreManager: ParametreManager
) {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    data class AuthState(
        val isLoggedIn: Boolean = false,
        val isInitialized: Boolean = false,
        val loginError: String? = null,
        val loggedInSite: SiteEntity? = null,
        val loggedInContrat: ContratEntity? = null,
        val availableSites: List<SiteEntity> = emptyList(),
        val isLoadingContracts: Boolean = false,
        val showValidationError: Boolean = false,
        val availableUtilisateursTps: List<UtilisateurTPEntity> = emptyList(),
        val loggedInUtilisateurTp: UtilisateurTPEntity? = null
    )

    private suspend fun getCurrentLocation(): Pair<Double?, Double?> = withContext(Dispatchers.IO) {
        try {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                Log.w("AUTH_MANAGER", "Permissions de localisation non accordées")
                return@withContext Pair(null, null)
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val availableProviders = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).filter { locationManager.isProviderEnabled(it) }

            if (availableProviders.isEmpty()) {
                Log.w("AUTH_MANAGER", "Aucun provider de localisation activé")
                return@withContext Pair(null, null)
            }

            // Demander une position fraîche sur le provider le plus rapide (NETWORK en priorité)
            // Timeout 5 s pour ne pas bloquer le démarrage
            val freshLocation = withTimeoutOrNull(5_000L) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) continuation.resume(location) {}
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                        }

                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    locationManager.requestSingleUpdate(availableProviders.first(), listener, Looper.getMainLooper())
                    continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
                }
            }

            if (freshLocation != null) {
                Log.d(
                    "AUTH_MANAGER",
                    "Position fraîche (${freshLocation.provider}): lat=${freshLocation.latitude}, lng=${freshLocation.longitude}"
                )
                return@withContext Pair(freshLocation.latitude, freshLocation.longitude)
            }

            // Fallback : dernière position en cache sur tous les providers
            @SuppressLint("MissingPermission")
            val cachedLocation = availableProviders
                .mapNotNull { locationManager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }

            if (cachedLocation != null) {
                Log.d(
                    "AUTH_MANAGER",
                    "Position en cache (${cachedLocation.provider}): lat=${cachedLocation.latitude}, lng=${cachedLocation.longitude}"
                )
                return@withContext Pair(cachedLocation.latitude, cachedLocation.longitude)
            }

            Log.w("AUTH_MANAGER", "Aucune position disponible")
            return@withContext Pair(null, null)

        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de la récupération de la localisation", e)
            return@withContext Pair(null, null)
        }
    }

    /**
     * Met à jour la position (lat/long) du device en attente d'affectation, via son identifiant
     * (le même qu'à la connexion, `ANDROID_ID`). **Best-effort** : un échec (réseau, position
     * indisponible, HTTP KO) reste **invisible pour l'utilisateur** et n'est **que loggé**.
     */
    private suspend fun updateDeviceLocationSilently(identifiantDevice: String) {
        try {
            val (lat, lng) = getCurrentLocation()
            // Position indisponible (services de localisation désactivés) → on envoie quand même
            // l'appel avec 0.0 en repli, pour que le device soit tracé côté back.
            if (lat == null || lng == null) {
                Log.w("AUTH_MANAGER", "Position indisponible → MAJ position device envoyée avec 0.0")
            }
            val latitude = lat ?: 0.0
            val longitude = lng ?: 0.0

            val response = smartphonesApi.updateSmartphoneLocation(
                UpdateSmartphoneLocationRequest(
                    identifiantDevice = identifiantDevice,
                    latitude = latitude,
                    longitude = longitude
                )
            )
            if (response.isSuccessful) {
                Log.d("AUTH_MANAGER", "Position device mise à jour (lat: $latitude, lng: $longitude)")
            } else {
                Log.e("AUTH_MANAGER", "Échec MAJ position device : code ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Échec MAJ position device", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun initializeApp() {
        try {
            try {
                // Récupérer l'identifiant unique du device
                val identifiantDevice = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                // D'abord, vérifier si le smartphone existe
                val checkResponse = smartphonesApi.checkSmartphoneExists(identifiantDevice)

                if (checkResponse.isSuccessful) {
                    val smartphoneExists = checkResponse.body() ?: false
                    Log.d("AUTH_MANAGER", "Vérification smartphone avec ID $identifiantDevice: $smartphoneExists")

                    if (!smartphoneExists) {
                        Log.d("AUTH_MANAGER", "smartphone inexistant, début de la procédure de creation")

                        // Récupérer la localisation actuelle
                        val (latitude, longitude) = getCurrentLocation()

                        // Créer le smartphone
                        val creerSmartphoneRequest = CreerSmartphoneMobileRequest(
                            numSerie = identifiantDevice,
                            identifiantDevice = identifiantDevice,
                            nom = "${Build.MANUFACTURER} ${Build.MODEL}",
                            typeTerminal = getDeviceModel(),
                            longitude = longitude,
                            latitude = latitude
                        )

                        val creerResponse = smartphonesApi.creerSmartphone(creerSmartphoneRequest)

                        if (creerResponse.isSuccessful) {
                            Log.d("AUTH_MANAGER", "Smartphone créé avec succès (lat: $latitude, lng: $longitude)")
                        } else {
                            Log.e(
                                "AUTH_MANAGER",
                                "Erreur lors de la création du smartphone: ${creerResponse.code()}"
                            )
                        }
                    }

                    val requete = LoginMobileRequest(idMobile = identifiantDevice)
                    val reponse = authApi.authenticateUser(loginMobileRequest = requete)

                    if (reponse.isSuccessful) {
                        val donnees = reponse.body()
                        tokenStore.token = donnees?.get("token") ?: ""
                        Log.d("AUTH_MANAGER", "Token récupéré avec succès: ${tokenStore.token}")

                        val contratDao = database.contratDao()
                        val utilisateurTPDao = database.utilisateurTPDao()
                        if (contratDao.count() != 1L ||utilisateurTPDao.count() <= 0) {
                            contratDao.purge()
                            loadContractsFromApi()
                        }
                    } else {
                        Log.e("AUTH_MANAGER", "Erreur HTTP API Init: ${reponse.code()}")
                        // Device en attente d'affectation : on rafraîchit sa position (best-effort,
                        // invisible pour l'utilisateur — toute erreur est seulement loggée).
                        updateDeviceLocationSilently(identifiantDevice)
                        // Afficher le message de validation
                        _authState.value = _authState.value.copy(
                            isInitialized = true,
                            showValidationError = true,
                        )
                        return // Sortir de la fonction sans continuer
                    }
                } else {
                    Log.e("AUTH_MANAGER", "Erreur lors de la vérification du smartphone: ${checkResponse.code()}")
                    throw Exception("Erreur lors de la vérification du smartphone: ${checkResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("AUTH_MANAGER", "Erreur lors de l'initialisation", e)
                _authState.value = _authState.value.copy(
                    isInitialized = true,
                    showValidationError = true,
                )
            }
            val contratDao = database.contratDao()
            val siteDao = database.siteDao()

            val nbContrats = contratDao.count()
            Log.d("AUTH_MANAGER", "Nombre de contrats en BDD : $nbContrats")

            if (nbContrats == 0L) {
                Log.d("AUTH_MANAGER", "0 contrat trouvé, purge de la table Site...")
                siteDao.purge()
            }
            database.utilisateurTPDao().getAllUtilisateurTPFlow().collect { utilisteursTps ->

                database.siteDao().getAllSitesFlow().collect { sites ->
                    _authState.value = _authState.value.copy(
                        availableSites = sites,
                        availableUtilisateursTps = utilisteursTps,
                        isInitialized = true,
                        isLoadingContracts = false
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de l'initialisation", e)
            _authState.value = _authState.value.copy(
                isInitialized = true,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadContractsFromApi() {
        try {
            _authState.value = _authState.value.copy(isLoadingContracts = true)
            Log.d("AUTH_MANAGER", "Récupération des contrats depuis l'API...")
            syncManager.synchroDescendante()
            Log.d("AUTH_MANAGER", "Récupération des contrats depuis l'API fini")
        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de la récupération des contrats", e)
        } finally {
            _authState.value = _authState.value.copy()
        }
    }

    private suspend fun MockAdmin() {
        val utilisateurTPDao = database.utilisateurTPDao()
        utilisateurTPDao.purge()
        val utilisateurTP = UtilisateurTPEntity(
            login = "admin",
            pin = "1234"
        )
        utilisateurTPDao.insertUtilisateur(utilisateurTP)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun saveContractToDatabase(contratDmo: com.idbat.mobile.generated.client.model.ContratDmo) {
        try {
            val contratDao = database.contratDao()
            val siteDao = database.siteDao()
            val matiereSiteDao = database.matiereSiteDao()
            val contratEvenementDao = database.contratEvenementDao()

            matiereSiteDao.purge()
            siteDao.purge()
            contratEvenementDao.clearEvenements()
            contratDao.purge()


            val contratEntity = ContratEntity(
                id = contratDmo.id ?: 0,
                trigramme = contratDmo.trigramme ?: "",
                nom = contratDmo.nom ?: "",
                hasPuce = contratDmo.hasPuce,
                hasCodebarres = contratDmo.hasCodebarres,
                hasImmatriculation = contratDmo.hasImmatriculation,
                hasSelectionusager = contratDmo.hasSelectionusager,
                hasSignatureParticuliers = contratDmo.hasSignatureparticuliers,
                hasSignatureProfessionnels = contratDmo.hasSignatureprofessionels,
                hasAccessSimpleParticuliers = contratDmo.hasAccessimpleparticuliers,
                hasAccessSimpleProfessionnels = contratDmo.hasAccessimpleprofessionels,
                hasPrepaiementParticuliers = contratDmo.hasPrepaiementParticuliers,
                hasPrepaiementProfessionnels = contratDmo.hasPrepaiementProfessionnels,
                hasGratuitParticuliers = contratDmo.hasGratuitParticuliers,
                hasGratuitProfessionels = contratDmo.hasGratuitProfessionels,
                hasPaimentComptantParticuliers = contratDmo.hasPaimentComptantParticuliers,
                hasPaimentComptantProfessionels = contratDmo.hasPaimentComptantProfessionels
            )

            contratDao.insertContrat(contratEntity)
            Log.d("AUTH_MANAGER", "Contrat sauvegardé: ${contratEntity.nom}")

            withContext(Dispatchers.IO) {
                val sitesJob = async {

                    contratDmo.contratSite?.let { sitesDmo ->
                        val sitesEntities = sitesDmo.map { siteDmo ->
                            val siteEntity = SiteEntity(
                                id = siteDmo.id ?: 0,
                                trigramme = siteDmo.trigramme ?: "",
                                nom = siteDmo.nom ?: "",
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
                            // On peuple le champ ignorer par Room pour un usage applicatif direct
                            siteEntity.matieres = siteDmo.matieres?.map { matiereDmo ->
                                MatiereSiteEntity(
                                    siteId = siteDmo.id ?: 0,
                                    matiereId = matiereDmo.id ?: 0,
                                    libelle = matiereDmo.libelle ?: "",
                                    unitesDesApportId = matiereDmo.unitesDesApportId,
                                    unitesDesApportLibelle = matiereDmo.unitesDesApportLibelle,
                                    isEnable = matiereDmo.isEnable
                                )
                            } ?: emptyList()
                            siteEntity
                        }

                        siteDao.insertSites(sitesEntities)
                        Log.d(
                            "AUTH_MANAGER",
                            "Sauvegarde de ${sitesEntities.size} sites pour le contrat ${contratEntity.nom}"
                        )

                        val allMatieres = mutableListOf<MatiereSiteEntity>()
                        sitesDmo.forEach { siteDmo ->
                            siteDmo.matieres?.map { matiereDmo ->
                                MatiereSiteEntity(
                                    siteId = siteDmo.id ?: 0,
                                    matiereId = matiereDmo.id ?: 0,
                                    libelle = matiereDmo.libelle ?: "",
                                    unitesDesApportId = matiereDmo.unitesDesApportId,
                                    unitesDesApportLibelle = matiereDmo.unitesDesApportLibelle,
                                    tarif = matiereDmo.tarif.toDouble(),
                                    isEnable = matiereDmo.isEnable
                                )
                            }?.let { allMatieres.addAll(it) }
                        }

                        matiereSiteDao.insertMatieres(allMatieres)
                        Log.d(
                            "AUTH_MANAGER",
                            "Sauvegarde de ${allMatieres.size} matières pour les sites du contrat ${contratEntity.nom}"
                        )

                    } ?: run {
                        Log.w("AUTH_MANAGER", "Aucun site associé trouvé pour le contrat ${contratEntity.nom}")
                    }
                }
                val utilisateursJob = async {
                    val utilisateurTPDao = database.utilisateurTPDao()
                    utilisateurTPDao.purge()


                    contratDmo.contratUtilisateursTps?.let { contratUtilisateursTpDmos ->
                        val utilisateurTPEntities = contratUtilisateursTpDmos.map { contratUtilisateursTpDmo ->
                            UtilisateurTPEntity(
                                id = contratUtilisateursTpDmo.id ?: 0,
                                login = contratUtilisateursTpDmo.login,
                                pin = contratUtilisateursTpDmo.motDePasse
                            )
                        }

                        utilisateurTPDao.insertUtilisateurs(utilisateurTPEntities)
                        Log.d(
                            "AUTH_MANAGER",
                            "Sauvegarde de ${utilisateurTPEntities.size} utilisateurTPEntities pour le contrat ${contratEntity.nom}"
                        )

                    } ?: run {
                        Log.w(
                            "AUTH_MANAGER",
                            "Aucun utilisateurTPEntitie associé trouvé pour le contrat ${contratEntity.nom}"
                        )
                    }
                }
                val usagersJob = async {
                    val usagersDao = database.usagerDao()
                    usagersDao.purge()
                    val carteContratDao = database.carteContratDao()
                    carteContratDao.clearCartes() // cascade-delete usager_cartes
                    val usagerCarteDao = database.usagerCarteDao()

                    contratDmo.contratUsagers?.let { contratUsagers ->
                        val usagerEntities = contratUsagers.map { contratUsager ->
                            UsagerEntity(
                                id = contratUsager.id ?: 0,
                                nom = contratUsager.nom,
                                prenom = contratUsager.prenom,
                                refClientIdBat = contratUsager.refClientIdBat,
                                contratId = contratUsager.contratId,
                                raisonSociale = contratUsager.raisonSociale,
                                typeApporteurLibelle = contratUsager.typeApporteurLibelle,
                                couriel = contratUsager.couriel,
                                typeApporteurIsPro = contratUsager.typeApporteurIsPro,
                            )
                        }

                        coroutineScope {
                            val jobs = usagerEntities.chunked(2000).map { lot ->
                                async { usagersDao.insertUsagers(lot) }
                            }
                            jobs.awaitAll()
                        }

                        val allCartes = contratUsagers.flatMap { contratUsager ->
                            contratUsager.cartes.map { carteDmo ->
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
                                        Date.from(
                                            it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                        )
                                    },
                                    dtSortieListeNoire = carteDmo.dtSortieListeNoire?.let {
                                        Date.from(
                                            it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                        )
                                    },
                                    motifListeNoireContratId = carteDmo.motifslistenoireContratId,
                                    motifListeNoireLibelle = carteDmo.motifslistenoireLibelle
                                )
                            }
                        }
                        carteContratDao.insertCartes(allCartes)

                        val allUsagerCartes = contratUsagers.flatMap { contratUsager ->
                            contratUsager.cartes.map { carteDmo ->
                                UsagerCarteEntity(
                                    usagerId = contratUsager.id ?: 0,
                                    carteId = carteDmo.id,
                                    dateDebut = Date.from(
                                        carteDmo.dateDebutAffectation
                                            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                    ),
                                    dateFin = carteDmo.dateFinAffectation?.let {
                                        Date.from(
                                            it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                        )
                                    }
                                )
                            }
                        }
                        usagerCarteDao.insertUsagerCartes(allUsagerCartes)

                        val seuilEtatDao = database.seuilEtatDao()
                        seuilEtatDao.clearSeuils()
                        val allSeuils = contratUsagers.flatMap { contratUsager ->
                            contratUsager.seuils.map { seuilDmo ->
                                SeuilEtatEntity(
                                    usagerId = contratUsager.id ?: 0,
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

                        Log.d(
                            "AUTH_MANAGER",
                            "Sauvegarde de ${usagerEntities.size} usagers + ${allCartes.size} cartes + ${allSeuils.size} seuils pour le contrat ${contratEntity.nom}"
                        )
                    } ?: run {
                        Log.w("AUTH_MANAGER", "Aucun usager associé trouvé pour le contrat ${contratEntity.nom}")
                    }
                }
                val evenementsJob = async {
                    val evenementDao = database.contratEvenementDao()
                    evenementDao.clearEvenements()

                    contratDmo.evenementsContrat?.let { evenementsDmo ->
                        val evenementEntities = evenementsDmo.map { evenementDmo ->
                            ContratEvenementEntity(
                                evenementId = evenementDmo.evenementId,
                                libelle = evenementDmo.libelle,
                                jointureId = evenementDmo.jointureId,
                                contratId = contratEntity.id,
                                isEnable = evenementDmo.isEnable
                            )
                        }

                        coroutineScope {
                            val lots = evenementEntities.chunked(500)
                            val jobs = lots.map { lotDe500 ->
                                async {
                                    evenementDao.insertEvenements(lotDe500)
                                }
                            }
                            jobs.awaitAll()
                        }

                        Log.d(
                            "AUTH_MANAGER",
                            "Sauvegarde de ${evenementEntities.size} ContratEvenementEntity pour le contrat ${contratEntity.nom}"
                        )
                    } ?: run {
                        Log.w("AUTH_MANAGER", "Aucun evenement associé trouvé pour le contrat ${contratEntity.nom}")
                    }
                }

                awaitAll(utilisateursJob, usagersJob, sitesJob, evenementsJob)
                val lastSynchroHistoryDao = database.lastSynchroHistoryDao()
                val dateExec = Date()
                contratDmo.contratSite?.let { sitesDmo ->
                    sitesDmo.forEach { siteDmo ->
                        lastSynchroHistoryDao.deleteTypeForSite(siteDmo.id, TypeSynchro.RECEPTION)
                        lastSynchroHistoryDao.insertSynchro(
                            LastSynchroHistoryEntity(
                                siteId = siteDmo.id,
                                date = dateExec,
                                type = TypeSynchro.RECEPTION,
                                operationsTentees = 1,
                                operationsReussies = 1
                            )
                        )
                    }
                }
                Log.d("SYNC", "Toutes les insertions parallèles sont terminées avec succès !")
            }


        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur sauvegarde contrat et sites en BDD", e)
        }
    }

    suspend fun login(username: String, password: String, siteId: Long?) {
        try {
            val utilisateurDao = database.utilisateurTPDao()
            val utilisateur = utilisateurDao.getUtilisateurByLogin(username)

            if (utilisateur != null && utilisateur.pin == password && siteId != null) {
                val selectedSite = _authState.value.availableSites.find { it.id == siteId }
                val contrat = selectedSite?.let { database.contratDao().getContratById(it.contratId) }
                _authState.value = _authState.value.copy(
                    isLoggedIn = true,
                    loggedInSite = selectedSite,
                    loggedInContrat = contrat,
                    loggedInUtilisateurTp = utilisateur
                )
                Log.d("AUTH_MANAGER", "Connexion réussie pour ${selectedSite?.nom}")
            } else {
                _authState.value = _authState.value.copy(
                    loginError = "Identifiants incorrects",
                )
            }
        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de la connexion", e)
            _authState.value = _authState.value.copy(
                loginError = "Erreur de connexion",
            )
        }
    }

    fun logout() {
        _authState.value = AuthState(
            isInitialized = true,
            availableSites = _authState.value.availableSites,
            availableUtilisateursTps = _authState.value.availableUtilisateursTps
        )
    }

    // --- Reconnexion obligatoire au retour de veille -------------------------------------
    // Horloge monotone (deep sleep inclus) au passage en arrière-plan → délai d'inactivité.
    private var backgroundedAtElapsed: Long? = null

    // Horloge murale (epoch millis) au passage en arrière-plan → détection du changement de jour.
    private var backgroundedAtWallClock: Long? = null

    /** À appeler quand l'app passe en arrière-plan/veille : mémorise les horodatages si connecté. */
    fun onAppBackgrounded() {
        if (_authState.value.isLoggedIn) {
            backgroundedAtElapsed = SystemClock.elapsedRealtime()
            backgroundedAtWallClock = System.currentTimeMillis()
        } else {
            backgroundedAtElapsed = null
            backgroundedAtWallClock = null
        }
    }

    /**
     * À appeler au retour au premier plan. Force la reconnexion (retour écran de login) si :
     *  1. premier accès de la journée (jour calendaire différent de la mise en veille), ou
     *  2. délai d'inactivité dépassé (paramètre global `FRONT_DELAI_INACTIVITE_MINUTES`,
     *     cf. [ParametreManager.sessionTimeoutMinutes]).
     */
    fun onAppForegrounded() {
        val backgroundedElapsed = backgroundedAtElapsed
        val backgroundedWallClock = backgroundedAtWallClock
        backgroundedAtElapsed = null
        backgroundedAtWallClock = null
        if (!_authState.value.isLoggedIn) return

        // 1. Premier accès de la journée : changement de jour calendaire (indépendant du paramètre).
        if (backgroundedWallClock != null &&
            !isSameCalendarDay(backgroundedWallClock, System.currentTimeMillis())
        ) {
            Log.d("AUTH_MANAGER", "Premier accès du jour → reconnexion obligatoire")
            logout()
            return
        }

        // 2. Délai d'inactivité dépassé.
        if (backgroundedElapsed != null) {
            val elapsedMs = SystemClock.elapsedRealtime() - backgroundedElapsed
            val timeoutMs = parametreManager.sessionTimeoutMinutes.value * 60_000L
            if (elapsedMs >= timeoutMs) {
                Log.d("AUTH_MANAGER", "Délai d'inactivité dépassé (${elapsedMs}ms) → reconnexion obligatoire")
                logout()
            }
        }
    }

    /** Vrai si les deux instants (epoch millis) tombent le même jour calendaire local. */
    private fun isSameCalendarDay(millisA: Long, millisB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = millisA }
        val calB = Calendar.getInstance().apply { timeInMillis = millisB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
                calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    private fun getDeviceModel(): String {
        return try {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            val version = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT

            // Format: "Manufacturer Model (Android Version, API Level)"
            "$manufacturer $model (Android $version, API $sdk)"
        } catch (e: Exception) {
            Log.w("AUTH_MANAGER", "Erreur lors de la récupération du modèle du device", e)
            "Unknown Android Device"
        }
    }

    suspend fun refreshLoggedInContrat() {
        val site = _authState.value.loggedInSite ?: return
        val contrat = database.contratDao().getContratById(site.contratId)
        _authState.value = _authState.value.copy(loggedInContrat = contrat)
    }

    fun dismissValidationError() {
        _authState.value = _authState.value.copy()
    }
}