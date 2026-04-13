package com.idbat.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.idbat.mobile.data.entities.LastSynchroHistoryEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.TypeSynchro
import com.idbat.mobile.ui.screens.HomeScreen
import com.idbat.mobile.ui.screens.LoginScreen
import com.idbat.mobile.ui.theme.IdbatTheme
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdbatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var loginError by remember { mutableStateOf<String?>(null) }
                    
                    var availableSites by remember { mutableStateOf<List<SiteEntity>>(emptyList()) }
                    var loggedInSite by remember { mutableStateOf<SiteEntity?>(null) }
                    var lastSynchroDateEnvoi by remember { mutableStateOf<Date?>(null) }
                    var lastSynchroDateReception by remember { mutableStateOf<Date?>(null) }
                    
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        val database = (context.applicationContext as IdbatApplication).database
                        database.siteDao().getAllSitesFlow().collect { sites ->
                            availableSites = sites
                        }
                    }

                    // A chaque fois que le site de connexion change (ex: suite à un login)
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
                        // Utiliser la date la plus récente entre envoi et réception pour l'affichage global

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
                            onTransferClick = { ->
                                scope.launch {
                                    val dateExec = Date()
                                    val database = (context.applicationContext as IdbatApplication).database
                                    val dao = database.lastSynchroHistoryDao()
                                    val lastEnvoi = dao.getLastSynchroForSiteAndType(loggedInSite!!.id, TypeSynchro.ENVOI)
                                    val lastReception = dao.getLastSynchroForSiteAndType(loggedInSite!!.id, TypeSynchro.RECEPTION)

                                    if(lastEnvoi != null){
                                        lastEnvoi.date = dateExec
                                        dao.updateSynchro(lastEnvoi)
                                    }else{
                                        dao.insertSynchro(LastSynchroHistoryEntity(siteId = loggedInSite!!.id, date = dateExec, type = TypeSynchro.ENVOI))
                                    }

                                    if(lastReception != null){
                                        lastReception.date = dateExec
                                        dao.updateSynchro(lastReception)
                                    }else{
                                        dao.insertSynchro(LastSynchroHistoryEntity(siteId = loggedInSite!!.id, date = dateExec, type = TypeSynchro.RECEPTION))
                                    }
                                    lastSynchroDateEnvoi = dateExec
                                    lastSynchroDateReception = dateExec
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

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    IdbatTheme {
        LoginScreen(errorMessage = null, availableSites = emptyList(), onLoginClick = { _, _, _ -> })
    }
}
