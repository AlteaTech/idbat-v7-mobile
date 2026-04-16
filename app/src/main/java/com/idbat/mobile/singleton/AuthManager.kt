package com.idbat.mobile.singleton

import android.util.Log
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.UtilisateurTPEntity
import com.idbat.mobile.generated.client.model.LoginMobileRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.String

@Singleton
class AuthManager @Inject constructor(
    private val database: AppDatabase
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

    suspend fun initializeApp() {
        try {
            // 1. Authentification machine
            if (ConfigSingleton.webEnable) {
                val requete = LoginMobileRequest(idMobile = "identifiant")
                val reponse = ApiClient.authApi.authenticateUser(loginMobileRequest = requete)

                if (reponse.isSuccessful) {
                    val donnees = reponse.body()
                    ConfigSingleton.tokenApi = donnees?.get("token") ?: ""
                    Log.d("AUTH_MANAGER", "Token récupéré avec succès: ${ConfigSingleton.tokenApi}")
                
                // 1.5. Récupération des contrats depuis l'API
                loadContractsFromApi() // ✅ Suppression du "await"
            } else {
                Log.e("AUTH_MANAGER", "Erreur HTTP API Init: ${reponse.code()}")
            }
        }

        // 2. Vérification BDD
        val contratDao = database.contratDao()
        val siteDao = database.siteDao()

        val nbContrats = contratDao.count()
        Log.d("AUTH_MANAGER", "Nombre de contrats en BDD : $nbContrats")

        if (nbContrats == 0L) {
            Log.d("AUTH_MANAGER", "0 contrat trouvé, purge de la table Site...")
            siteDao.purge()
        }

        // 3. Charger les sites disponibles
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
                    // Convertir et sauvegarder en base de données
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

    private suspend fun MockAdmin(){
        val utilisateurTPDao = database.utilisateurTPDao()
        utilisateurTPDao.purge()
        val utilisateurTP = UtilisateurTPEntity(
            login= "admin",
            pin="1234"
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
        // 1. Conversion et sauvegarde du contrat
        val contratEntity = ContratEntity(
            id = contratDmo.id ?: 0,
            trigramme = contratDmo.trigramme ?: "",
            nom = contratDmo.nom ?: ""
        )
        
        contratDao.insertContrat(contratEntity)
        Log.d("AUTH_MANAGER", "Contrat sauvegardé: ${contratEntity.nom}")
        
        // 2. Conversion et sauvegarde des sites associés
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
            
            // Sauvegarde en lot de tous les sites
            siteDao.insertSites(sitesEntities)
            Log.d("AUTH_MANAGER", "Sauvegarde de ${sitesEntities.size} sites pour le contrat ${contratEntity.nom}")
            
            // Log détaillé des sites sauvegardés
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
}