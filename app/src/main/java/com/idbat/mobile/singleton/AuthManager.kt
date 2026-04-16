package com.idbat.mobile.singleton

import android.util.Log
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.generated.client.model.LoginMobileRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

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
        val availableSites: List<SiteEntity> = emptyList()
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
                    Log.d("AUTH_MANAGER", "Token récupéré avec succès")
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
                    isInitialized = true
                )
            }

        } catch (e: Exception) {
            Log.e("AUTH_MANAGER", "Erreur lors de l'initialisation", e)
            _authState.value = _authState.value.copy(isInitialized = true)
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