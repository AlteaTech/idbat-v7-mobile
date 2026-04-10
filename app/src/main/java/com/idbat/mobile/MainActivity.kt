package com.idbat.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.idbat.mobile.ui.screens.LoginScreen
import com.idbat.mobile.ui.screens.SuccessScreen
import com.idbat.mobile.ui.theme.IdbatTheme
import kotlinx.coroutines.launch

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
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current

                    if (isLoggedIn) {
                        SuccessScreen()
                    } else {
                        LoginScreen(
                            errorMessage = loginError,
                            onLoginClick = { username, password ->
                                scope.launch {
                                    val database = (context.applicationContext as IdbatApplication).database
                                    val utilisateurDao = database.utilisateurTPDao()
                                    
                                    val utilisateur = utilisateurDao.getUtilisateurByLogin(username)
                                    
                                    if (utilisateur != null && utilisateur.pin == password) {
                                        // Connexion réussie
                                        loginError = null
                                        
                                        // Mettre à jour la date de dernière connexion
                                        utilisateurDao.insertUtilisateur(utilisateur.copy(lastLoginDate = System.currentTimeMillis()))
                                        
                                        isLoggedIn = true
                                    } else {
                                        // Identifiants incorrects
                                        loginError = "Identifiant ou mot de passe incorrect"
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
        LoginScreen(errorMessage = null, onLoginClick = { _, _ -> })
    }
}
