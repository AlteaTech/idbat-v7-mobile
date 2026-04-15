package com.idbat.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.TypeSynchro
import com.idbat.mobile.generated.client.model.LoginMobileRequest
import com.idbat.mobile.singleton.ApiClient
import com.idbat.mobile.singleton.ConfigSingleton
import com.idbat.mobile.ui.screens.HomeScreen
import com.idbat.mobile.ui.screens.LoginScreen
import com.idbat.mobile.ui.theme.IdbatTheme
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdbatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- ETAT D'INITIALISATION ---
                    // Vaut false tant que l'API et la vérification BDD ne sont pas terminées
                    var isAppInitialized by remember { mutableStateOf(false) }

                    // Vos variables d'état existantes
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var loginError by remember { mutableStateOf<String?>(null) }
                    var availableSites by remember { mutableStateOf<List<SiteEntity>>(emptyList()) }
                    var loggedInSite by remember { mutableStateOf<SiteEntity?>(null) }
                    var lastSynchroDateEnvoi by remember { mutableStateOf<Date?>(null) }
                    var lastSynchroDateReception by remember { mutableStateOf<Date?>(null) }

                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current

                    // --- LOGIQUE DE DEMARRAGE (API + BDD) ---
                    LaunchedEffect(Unit) {
                        val database = (context.applicationContext as IdbatApplication).database

                        // 1. Appel API d'authentification machine
                        try {
                            if (ConfigSingleton.webEnable) {
                                val requete = LoginMobileRequest(idMobile = "identifiant")
                                val reponse = ApiClient.authApi.authenticateUser(loginMobileRequest = requete)

                                if (reponse.isSuccessful) {
                                    val donnees = reponse.body()
                                    // Utilisation de ?: pour éviter un crash (NullPointerException) si le token est absent
                                    ConfigSingleton.tokenApi = donnees?.get("token") ?: ""
                                    Log.d("INIT_APP", "Token récupéré avec succès")
                                } else {
                                    Log.e("INIT_APP", "Erreur HTTP API Init: ${reponse.code()}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("INIT_APP", "Erreur réseau API Init", e)
                        }

                        // 2. Vérification du nombre de contrats
                        try {
                            // Adaptez le nom du DAO et de la méthode selon votre code
                            val contratDao = database.contratDao()
                            val siteDao = database.siteDao()

                            val nbContrats = contratDao.count()
                            Log.d("INIT_APP", "Nombre de contrats en BDD : $nbContrats")

                            if (nbContrats == 0L) {
                                Log.d("INIT_APP", "0 contrat trouvé, purge de la table Site...")
                                siteDao.purge()
                            }
                        } catch (e: Exception) {
                            Log.e("INIT_APP", "Erreur lors de la vérification BDD", e)
                        }

                        // 3. Initialisation terminée, on affiche l'application
                        isAppInitialized = true
                    }

                    // --- GESTION DE L'AFFICHAGE ---

                    if (!isAppInitialized) {
                        // Écran de chargement affiché pendant l'appel API et la BDD
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // L'application normale démarre ici

                        LaunchedEffect(Unit) {
                            val database = (context.applicationContext as IdbatApplication).database
                            database.siteDao().getAllSitesFlow().collect { sites ->
                                availableSites = sites
                            }
                        }

                        LaunchedEffect(loggedInSite) {
                            loggedInSite?.let { site ->
                                val database = (context.applicationContext as IdbatApplication).database
                                val dao = database.lastSynchroHistoryDao()
                                val lastEnvoi = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.ENVOI)
                                val lastReception = dao.getLastSynchroForSiteAndType(site.id, TypeSynchro.RECEPTION)
                                lastSynchroDateEnvoi = lastEnvoi?.date
                                lastSynchroDateReception = lastReception?.date
                            }
                        }

                        if (isLoggedIn) {
                            HomeScreen(
                                selectedSite = loggedInSite,
                                lastSynchroDateEnvoi = lastSynchroDateEnvoi,
                                lastSynchroDateReception = lastSynchroDateReception,
                                onLogoutClick = {
                                    isLoggedIn = false
                                    loginError = null
                                    loggedInSite = null
                                    lastSynchroDateEnvoi = null
                                    lastSynchroDateReception = null
                                },
                                onTransferClick = {
                                    scope.launch {
                                        loggedInSite?.let { site ->
                                            Log.d("API_LOGIN", "Mon token est : ${ConfigSingleton.tokenApi}")

                                            val dateExec = Date()
                                            val database = (context.applicationContext as IdbatApplication).database
                                            val dao = database.lastSynchroHistoryDao()
                                            val lastEnvoi = dao.getLastSynchroForSiteAndType(loggedInSite!!.id, TypeSynchro.ENVOI)
                                            val lastReception = dao.getLastSynchroForSiteAndType(loggedInSite!!.id, TypeSynchro.RECEPTION)

                                            if (lastEnvoi != null) {
                                                lastEnvoi.date = dateExec
                                                dao.updateSynchro(lastEnvoi)
                                            } else {
                                                dao.insertSynchro(LastSynchroHistoryEntity(siteId = loggedInSite!!.id, date = dateExec, type = TypeSynchro.ENVOI))
                                            }

                                            if (lastReception != null) {
                                                lastReception.date = dateExec
                                                dao.updateSynchro(lastReception)
                                            } else {
                                                dao.insertSynchro(LastSynchroHistoryEntity(siteId = loggedInSite!!.id, date = dateExec, type = TypeSynchro.RECEPTION))
                                            }
                                            lastSynchroDateEnvoi = dateExec
                                            lastSynchroDateReception = dateExec
                                        }
                                    }
                                }
                            )
                        } else {
                            LoginScreen(
                                errorMessage = loginError,
                                availableSites = availableSites,
                                onLoginClick = { username, password, siteId ->
                                    scope.launch {
                                        val database = (context.applicationContext as IdbatApplication).database
                                        val utilisateurDao = database.utilisateurTPDao()

                                        val utilisateur = utilisateurDao.getUtilisateurByLogin(username)

                                        if (utilisateur != null && utilisateur.pin == password && siteId != null) {
                                            loginError = null
                                            utilisateurDao.insertUtilisateur(utilisateur.copy(lastLoginDate = System.currentTimeMillis()))

                                            loggedInSite = availableSites.find { it.id == siteId }
                                            isLoggedIn = true
                                        } else {
                                            loginError = "Identifiant, mot de passe ou site incorrect"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}