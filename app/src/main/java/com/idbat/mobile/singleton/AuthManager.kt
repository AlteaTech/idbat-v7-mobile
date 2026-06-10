package com.idbat.mobile.singleton

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.Build
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
    @ApplicationContext private val context: Context
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
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    locationManager.requestSingleUpdate(availableProviders.first(), listener, Looper.getMainLooper())
                    continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
                }
            }

            if (freshLocation != null) {
                Log.d("AUTH_MANAGER", "Position fraîche (${freshLocation.provider}): lat=${freshLocation.latitude}, lng=${freshLocation.longitude}")
                return@withContext Pair(freshLocation.latitude, freshLocation.longitude)
            }

            // Fallback : dernière position en cache sur tous les providers
            @SuppressLint("MissingPermission")
            val cachedLocation = availableProviders
                .mapNotNull { locationManager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }

            if (cachedLocation != null) {
                Log.d("AUTH_MANAGER", "Position en cache (${cachedLocation.provider}): lat=${cachedLocation.latitude}, lng=${cachedLocation.longitude}")
                return@withContext Pair(cachedLocation.latitude, cachedLocation.longitude)
            }

            Log.w("AUTH_MANAGER", "Aucune position disponible")
            return@withContext Pair(null, null)

        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de la récupération de la localisation", e)
            return@withContext Pair(null, null)
        }
    }

    suspend fun initializeApp() {
        try {
            try {
                if (ConfigSingleton.webEnable) {
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
                            val siteDao = database.siteDao()
                            if (contratDao.count() == 0L || siteDao.count() == 0L) {
                                loadContractsFromApi()
                            }
                        } else {
                            Log.e("AUTH_MANAGER", "Erreur HTTP API Init: ${reponse.code()}")
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
                if (!ConfigSingleton.webEnable) {
                    mockSites()
                    mockUsagers()
                    mockCartes()
                    insertMockEvenements()
                }
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

    private suspend fun loadContractsFromApi() {
        try {
            _authState.value = _authState.value.copy(isLoadingContracts = true)
            Log.d("AUTH_MANAGER", "Récupération des contrats depuis l'API...")

            val response = contratsApi.getByDevice()

            if (response.isSuccessful) {
                val contratDmo = response.body()
                Log.d("AUTH_MANAGER", "Contrats récupérés avec succès: ${contratDmo?.nom}")

                contratDmo?.let { dmo ->
                    saveContractToDatabase(dmo)
                }
            } else {
                Log.e("AUTH_MANAGER", "Erreur récupération contrats: ${response.code()}")
                Log.e("AUTH_MANAGER", "Message d'erreur: ${response.errorBody()?.string()}")
            }
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

    private suspend fun insertMockEvenements() {
        val mockEvenements = listOf(
            "Départ de feu",
            "Fuite de produit toxique",
            "Benne endommagée",
            "Portail bloqué",
            "Intrusion",
            "Déversement sauvage",
            "Panne presse",
            "Accident voyageur",
            "Matériel manquant",
            "Odeur suspecte",
            "Animal mort",
            "Panne éclairage",
            "Problème signalétique",
            "Défaut de tri",
            "Casse barrière"
        )

        val contratEvenementDao = database.contratEvenementDao()
        // On insère les mocks pour le contratId 1 (qui est le contrat par défaut)
        mockEvenements.forEachIndexed { index, libelle ->
            val  ContratEvenement = ContratEvenementEntity(
                evenementId = (index + 1).toLong(),
                libelle = libelle,
                jointureId = (index + 1).toLong(),
                contratId = 1L
            )
            contratEvenementDao.insertEvenement(ContratEvenement)
        }
    }
    private suspend fun mockSites() {
        val contratDao = database.contratDao()
        val siteDao = database.siteDao()

        val contrat = ContratEntity(
            id = 1L,
            trigramme = "VEO",
            nom = "Veolia Recyclage",
            hasCodebarres = true,
            hasPuce = true,
            hasImmatriculation = true,
            hasSelectionusager = true,
            hasSignatureParticuliers = true,
            hasSignatureProfessionnels = false
        )
        contratDao.insertContrat(contrat)

        siteDao.insertSites(
            listOf(
                SiteEntity(
                    id = 1L,
                    trigramme = "MAR",
                    nom = "Déchetterie Marseille Nord",
                    adresse1 = "12 Rue des Abeilles",
                    adresse2 = null,
                    codePostal = "13014",
                    ville = "Marseille",
                    typeImprimante = null,
                    macImprimante = null,
                    horairesOuverture = "Lun-Sam 8h-18h",
                    destinatairesMailTransfertTP = null,
                    contratId = 1L
                ),
                SiteEntity(
                    id = 2L,
                    trigramme = "LYO",
                    nom = "Centre de tri Lyon Sud",
                    adresse1 = "47 Avenue de la Confluence",
                    adresse2 = null,
                    codePostal = "69007",
                    ville = "Lyon",
                    typeImprimante = null,
                    macImprimante = null,
                    horairesOuverture = "Lun-Ven 7h-17h",
                    destinatairesMailTransfertTP = null,
                    contratId = 1L
                )
            )
        )
        val matiereSiteDao = database.matiereSiteDao()
        matiereSiteDao.insertMatieres(
            listOf(
                MatiereSiteEntity(matiereId = 1L, siteId = 1L, libelle = "Ferrailles",      unitesDesApportId = 1L, unitesDesApportLibelle = "Kg",  tarif = 0.50),
                MatiereSiteEntity(matiereId = 2L, siteId = 1L, libelle = "Gravats 170107",  unitesDesApportId = 2L, unitesDesApportLibelle = "m3",  tarif = 0.10),
                MatiereSiteEntity(matiereId = 3L, siteId = 2L, libelle = "Cartons",         unitesDesApportId = 1L, unitesDesApportLibelle = "Kg",  tarif = 0.30),
                MatiereSiteEntity(matiereId = 4L, siteId = 2L, libelle = "Plastiques",      unitesDesApportId = 2L, unitesDesApportLibelle = "m3",  tarif = 0.25)
            )
        )
        Log.d("AUTH_MANAGER", "2 sites mock insérés avec 2 matières chacun")
    }
    private suspend fun mockUsagers() {
        val usagerDao = database.usagerDao()
        usagerDao.purge()
        usagerDao.insertUsager(
            UsagerEntity(
                id = 1L,
                nom = "VIDAL",
                prenom = "jérémie",
                contratId = 1L,
                refClientIdBat = 123456,
                typeApporteurIsPro = false,
                couriel = "jeremie.vidal@example.com"
            )
        )
        usagerDao.insertUsager(
            UsagerEntity(
                id = 2L,
                nom = "lan",
                prenom = "alicia",
                contratId = 1L,
                refClientIdBat = 123457,
                typeApporteurIsPro = false
            )
        )
        usagerDao.insertUsager(
            UsagerEntity(
                id = 3L,
                nom = "rosier",
                prenom = "ronald",
                contratId = 1L,
                refClientIdBat = 13,
                typeApporteurIsPro = true
            )
        )
        Log.d("AUTH_MANAGER", "3 Usagers mock insérés")
    }

    private suspend fun mockCartes() {
        val dao = database.carteContratDao()
        dao.clearCartes()

        val cartes = listOf(
            // Usager 1 : VIDAL jérémie (id=1)
            CarteContratEntity(id = 1L,  libelle = "Carte Puce VIDAL",  type = "P", valeur = null,    uidRfid = "A1B2C3D4", isCreationByQRCode = false, carteGriseJ1 = null,      carteGriseF3 = null, contratId = 1L, carteId = 1L),
            CarteContratEntity(id = 2L,  libelle = "Carte CB VIDAL",    type = "C", valeur = "123456", uidRfid = null,        isCreationByQRCode = false, carteGriseJ1 = null,      carteGriseF3 = null, contratId = 1L, carteId = 1L),
            CarteContratEntity(id = 3L,  libelle = "Carte Immat VIDAL", type = "I", valeur = "GT977GW",     uidRfid = null,        isCreationByQRCode = false, carteGriseJ1 = "AB123CD", carteGriseF3 = null, contratId = 1L, carteId = 1L),
            // Usager 2 : lan alicia (id=2)
            CarteContratEntity(id = 4L,  libelle = "Carte Puce LAN",    type = "P", valeur = null,    uidRfid = "E5F6A7B8", isCreationByQRCode = false, carteGriseJ1 = null,      carteGriseF3 = null, contratId = 1L, carteId = 2L),
            CarteContratEntity(id = 5L,  libelle = "Carte CB LAN",      type = "C", valeur = "123457", uidRfid = null,        isCreationByQRCode = false, carteGriseJ1 = null,      carteGriseF3 = null, contratId = 1L, carteId = 2L),
            CarteContratEntity(id = 6L,  libelle = "Carte Immat LAN",   type = "I", valeur = "GT978GW",     uidRfid = null,        isCreationByQRCode = false, carteGriseJ1 = "EF456GH", carteGriseF3 = null, contratId = 1L, carteId = 2L),
            // Usager 3 : rosier ronald (id=3)
            CarteContratEntity(id = 7L,  libelle = "Carte Puce ROSIER",  type = "P", valeur = null,   uidRfid = "C9D0E1F2", isCreationByQRCode = false, carteGriseJ1 = null,      carteGriseF3 = null, contratId = 1L, carteId = 3L),
            CarteContratEntity(id = 8L,  libelle = "Carte CB ROSIER",    type = "C", valeur = "13",   uidRfid = null,        isCreationByQRCode = false, carteGriseJ1 = null,      carteGriseF3 = null, contratId = 1L, carteId = 3L),
            CarteContratEntity(id = 9L,  libelle = "Carte Immat ROSIER", type = "I", valeur = "GT979GW",    uidRfid = null,        isCreationByQRCode = false, carteGriseJ1 = "IJ789KL", carteGriseF3 = null, contratId = 1L, carteId = 3L),
        )

        dao.insertCartes(cartes)
        Log.d("AUTH_MANAGER", "9 cartes mock insérées (3 par usager : P, C, I)")

        val usagerCarteDao = database.usagerCarteDao()
        usagerCarteDao.clearUsagerCartes()
        val dateDebut = GregorianCalendar(2000, Calendar.JANUARY, 1).time
        val dateFin = GregorianCalendar(3000, Calendar.DECEMBER, 31).time
        val liens = listOf(
            UsagerCarteEntity(usagerId = 1L, carteId = 1L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 1L, carteId = 2L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 1L, carteId = 3L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 2L, carteId = 4L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 2L, carteId = 5L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 2L, carteId = 6L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 3L, carteId = 7L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 3L, carteId = 8L, dateDebut = dateDebut, dateFin = dateFin),
            UsagerCarteEntity(usagerId = 3L, carteId = 9L, dateDebut = dateDebut, dateFin = dateFin),
        )
        usagerCarteDao.insertUsagerCartes(liens)
        Log.d("AUTH_MANAGER", "9 liens usager_cartes mock insérés")
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
                hasSignatureProfessionnels = contratDmo.hasSignatureprofessionels
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
                                    unitesDesApportLibelle = matiereDmo.unitesDesApportLibelle
                                )
                            } ?: emptyList()
                            siteEntity
                        }

                        siteDao.insertSites(sitesEntities)
                        Log.d("AUTH_MANAGER", "Sauvegarde de ${sitesEntities.size} sites pour le contrat ${contratEntity.nom}")

                        val allMatieres = mutableListOf<MatiereSiteEntity>()
                        sitesDmo.forEach { siteDmo ->
                            siteDmo.matieres?.map { matiereDmo ->
                                MatiereSiteEntity(
                                    siteId = siteDmo.id ?: 0,
                                    matiereId = matiereDmo.id ?: 0,
                                    libelle = matiereDmo.libelle ?: "",
                                    unitesDesApportId = matiereDmo.unitesDesApportId,
                                    unitesDesApportLibelle = matiereDmo.unitesDesApportLibelle,
                                    tarif = matiereDmo.tarif.toDouble()
                                )
                            }?.let { allMatieres.addAll(it) }
                        }
                        
                        matiereSiteDao.insertMatieres(allMatieres)
                        Log.d("AUTH_MANAGER", "Sauvegarde de ${allMatieres.size} matières pour les sites du contrat ${contratEntity.nom}")

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
                                    carteId = carteDmo.id
                                )
                            }
                        }
                        carteContratDao.insertCartes(allCartes)

                        val allUsagerCartes = contratUsagers.flatMap { contratUsager ->
                            contratUsager.cartes.map { carteDmo ->
                                UsagerCarteEntity(
                                    usagerId = contratUsager.id ?: 0,
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
                                contratId = contratEntity.id
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
                        lastSynchroHistoryDao.deleteTypeForSite(siteDmo.id,TypeSynchro.RECEPTION)
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