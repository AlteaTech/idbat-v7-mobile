package com.idbat.mobile.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.idbat.mobile.singleton.AuthManager
import com.idbat.mobile.ui.components.ValidationErrorDialog
import com.idbat.mobile.ui.screens.MainScreen
import com.idbat.mobile.ui.theme.IdbatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IdbatTheme {
                val authState by authManager.authState.collectAsState()

                // Afficher la boîte de dialogue bloquante si nécessaire
                if (authState.showValidationError) {
                    ValidationErrorDialog()
                }

                // Votre contenu principal ici
                Surface(
                    modifier = Modifier.Companion.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // Afficher la boîte de dialogue bloquante si nécessaire
                    if (!authState.showValidationError) {
                        MainScreen()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // App en arrière-plan/veille : démarre le décompte d'inactivité si une session est ouverte.
        authManager.onAppBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        // Retour au premier plan : force la reconnexion si le délai d'inactivité est dépassé.
        authManager.onAppForegrounded()
    }
}