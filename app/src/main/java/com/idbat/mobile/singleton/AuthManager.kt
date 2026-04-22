package com.idbat.mobile.singleton

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.UtilisateurTPEntity
import com.idbat.mobile.generated.client.model.CreerSmartphoneMobileRequest
import com.idbat.mobile.generated.client.model.LoginMobileRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

@Singleton
class AuthManager @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    data class AuthState(
        val isLoggedIn: Boolean = false,
        val isInitialized: Boolean = false,
        val loginError: String? = null,
        val loggedInSite: SiteEntity? = null,
        val availableSites: List<SiteEntity> = emptyList(),
        val isLoadingContracts: Boolean = false
    )

    private suspend fun getCurrentLocation(): Pair<Double?, Double?> = withContext(Dispatchers.IO) {
        try {
            // Vérifier les permissions
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasCoarseLocation && !hasFineLocation) {
                Log.w("AUTH_MANAGER", "Permissions de localisation non accordées")
                return@withContext Pair(null, null)
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Vérifier si le GPS ou le réseau est disponible
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            
            if (!isGpsEnabled && !isNetworkEnabled) {
                Log.w("AUTH_MANAGER", "Aucun provider de localisation activé")
                return@withContext Pair(null, null)
            }

            // Essayer d'obtenir la dernière localisation connue
            val provider = when {
                isGpsEnabled -> LocationManager.GPS_PROVIDER
                isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
                else -> return@withContext Pair(null, null)
            }

            @SuppressLint("MissingPermission")
            val location = locationManager.getLastKnownLocation(provider)
            
            if (location != null) {
                Log.d("AUTH_MANAGER", "Localisation obtenue: lat=${location.latitude}, lng=${location.longitude}")
                return@withContext Pair(location.latitude, location.longitude)
            } else {
                Log.w("AUTH_MANAGER", "Aucune localisation connue disponible")
                return@withContext Pair(null, null)
            }
            
        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de la récupération de la localisation", e)
            return@withContext Pair(null, null)
        }
    }

    suspend fun initializeApp() {
        try {
            if (ConfigSingleton.webEnable) {
                // Récupérer l'identifiant unique du device
                val identifiantDevice = Settings.Secure.getString(
                    context.contentResolver, 
                    Settings.Secure.ANDROID_ID
                )
                
                // D'abord, vérifier si le smartphone existe
                val checkResponse = ApiClient.smartphonesApi.checkSmartphoneExists(identifiantDevice)
                
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
                            nom = "Smartphone Android",
                            typeTerminal = "ANDROID",
                            longitude = longitude,
                            latitude = latitude
                        )
                        
                        val creerResponse = ApiClient.smartphonesApi.creerSmartphone(creerSmartphoneRequest)
                        
                        if (creerResponse.isSuccessful) {
                            Log.d("AUTH_MANAGER", "Smartphone créé avec succès (lat: $latitude, lng: $longitude)")
                        } else {
                            Log.e("AUTH_MANAGER", "Erreur lors de la création du smartphone: ${creerResponse.code()}")
                        }
                    }
                    else{
                        val requete = LoginMobileRequest(idMobile = identifiantDevice)
                        val reponse = ApiClient.authApi.authenticateUser(loginMobileRequest = requete)

                        if (reponse.isSuccessful) {
                            val donnees = reponse.body()
                            ConfigSingleton.tokenApi = donnees?.get("token") ?: ""
                            Log.d("AUTH_MANAGER", "Token récupéré avec succès: ${ConfigSingleton.tokenApi}")

                            loadContractsFromApi()
                        } else {
                            Log.e("AUTH_MANAGER", "Erreur HTTP API Init: ${reponse.code()}")
                        }
                    }
                } else {
                    Log.e("AUTH_MANAGER", "Erreur lors de la vérification du smartphone: ${checkResponse.code()}")
                }
            } else {
                Log.d("AUTH_MANAGER", "Mode hors-ligne activé - Chargement des données mockées")
                loadMockDataToDatabase()
            }

            val contratDao = database.contratDao()
            val siteDao = database.siteDao()

            val nbContrats = contratDao.count()
            Log.d("AUTH_MANAGER", "Nombre de contrats en BDD : $nbContrats")

            if (nbContrats == 0L) {
                Log.d("AUTH_MANAGER", "0 contrat trouvé, purge de la table Site...")
                siteDao.purge()
            }

            database.siteDao().getAllSitesFlow().collect { sites ->
                _authState.value = _authState.value.copy(
                    availableSites = sites,
                    isInitialized = true,
                    isLoadingContracts = false
                )
            }

        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de l'initialisation", e)
            _authState.value = _authState.value.copy(
                isInitialized = true,
                isLoadingContracts = false
            )
        }
    }

    private suspend fun loadContractsFromApi() {
        try {
            _authState.value = _authState.value.copy(isLoadingContracts = true)
            Log.d("AUTH_MANAGER", "Récupération des contrats depuis l'API...")

            val response = ApiClient.contratsApi.getByDevice()

            if (response.isSuccessful) {
                val contratDmo = response.body()
                Log.d("AUTH_MANAGER", "Contrats récupérés avec succès: $contratDmo")

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
            _authState.value = _authState.value.copy(isLoadingContracts = false)
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

    private suspend fun saveContractToDatabase(contratDmo: com.idbat.mobile.generated.client.model.ContratDmo) {
        try {
            val contratDao = database.contratDao()
            val siteDao = database.siteDao()
            siteDao.purge()
            contratDao.purge()

            MockAdmin()

            val contratEntity = ContratEntity(
                id = contratDmo.id ?: 0,
                trigramme = contratDmo.trigramme ?: "",
                nom = contratDmo.nom ?: ""
            )

            contratDao.insertContrat(contratEntity)
            Log.d("AUTH_MANAGER", "Contrat sauvegardé: ${contratEntity.nom}")

            contratDmo.contratSite?.let { sitesDmo ->
                val sitesEntities = sitesDmo.map { siteDmo ->
                    SiteEntity(
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
                }

                siteDao.insertSites(sitesEntities)
                Log.d("AUTH_MANAGER", "Sauvegarde de ${sitesEntities.size} sites pour le contrat ${contratEntity.nom}")

                sitesEntities.forEach { site ->
                    Log.d("AUTH_MANAGER", "Site sauvegardé: ${site.nom} (ID: ${site.id})")
                }
            } ?: run {
                Log.w("AUTH_MANAGER", "Aucun site associé trouvé pour le contrat ${contratEntity.nom}")
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
                _authState.value = _authState.value.copy(
                    isLoggedIn = true,
                    loginError = null,
                    loggedInSite = selectedSite
                )
                Log.d("AUTH_MANAGER", "Connexion réussie pour ${selectedSite?.nom}")
            } else {
                _authState.value = _authState.value.copy(
                    loginError = "Identifiants incorrects"
                )
            }
        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de la connexion", e)
            _authState.value = _authState.value.copy(
                loginError = "Erreur de connexion"
            )
        }
    }

    fun logout() {
        _authState.value = AuthState(
            isInitialized = true,
            availableSites = _authState.value.availableSites
        )
    }

    private suspend fun loadMockDataToDatabase() {
        try {
            _authState.value = _authState.value.copy(isLoadingContracts = true)
            Log.d("AUTH_MANAGER", "Chargement des données mockées en base...")

            MockAdmin()
            val contratDao = database.contratDao()
            val siteDao = database.siteDao()
            if (contratDao.count() != 0L) {
                return
            }
            val contratsEntities = listOf(
                ContratEntity(
                    id = 1,
                    trigramme = "ABC",
                    nom = "Contrat ABC - Démonstration"
                )
            )

            contratsEntities.forEach { contrat ->
                contratDao.insertContrat(contrat)
                Log.d("AUTH_MANAGER", "Contrat mocké sauvegardé: ${contrat.nom}")
            }

            val sitesEntities = mutableListOf<SiteEntity>()

            sitesEntities.addAll(
                listOf(
                    SiteEntity(
                        id = 101,
                        trigramme = "ABC01",
                        nom = "Site ABC - Paris",
                        adresse1 = "123 Rue de la Paix",
                        adresse2 = "2ème étage",
                        codePostal = "75001",
                        ville = "Paris",
                        typeImprimante = "ZEBRA",
                        macImprimante = "00:11:22:33:44:55",
                        horairesOuverture = "08:00-18:00",
                        destinatairesMailTransfertTP = "admin@abc-paris.com",
                        contratId = 1
                    ),
                    SiteEntity(
                        id = 102,
                        trigramme = "ABC02",
                        nom = "Site ABC - Lyon",
                        adresse1 = "456 Place Bellecour",
                        adresse2 = null,
                        codePostal = "69002",
                        ville = "Lyon",
                        typeImprimante = "BROTHER",
                        macImprimante = "00:11:22:33:44:66",
                        horairesOuverture = "07:30-19:00",
                        destinatairesMailTransfertTP = "admin@abc-lyon.com",
                        contratId = 1
                    )
                )
            )

            sitesEntities.addAll(
                listOf(
                    SiteEntity(
                        id = 201,
                        trigramme = "XYZ01",
                        nom = "Site XYZ - Marseille",
                        adresse1 = "789 Vieux Port",
                        adresse2 = "Bâtiment A",
                        codePostal = "13001",
                        ville = "Marseille",
                        typeImprimante = "EPSON",
                        macImprimante = "00:11:22:33:44:77",
                        horairesOuverture = "09:00-17:00",
                        destinatairesMailTransfertTP = "contact@xyz-marseille.com",
                        contratId = 1
                    )
                )
            )

            siteDao.insertSites(sitesEntities)
            Log.d("AUTH_MANAGER", "Sites mockés sauvegardés: ${sitesEntities.size} sites")

            sitesEntities.forEach { site ->
                Log.d("AUTH_MANAGER", "Site mocké sauvegardé: ${site.nom} (${site.trigramme})")
            }

            Log.d("AUTH_MANAGER", "✅ Alimentation mockée terminée avec succès")

        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "❌ Erreur lors du chargement des données mockées", e)
        } finally {
            _authState.value = _authState.value.copy(isLoadingContracts = false)
        }
    }
}