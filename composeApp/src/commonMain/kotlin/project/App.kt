package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.screens.HomeScreen
import org.example.project.screens.LoginScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        // État simple pour gérer la connexion (à remplacer par un vrai gestionnaire d'état plus tard)
        var isLoggedIn by remember { mutableStateOf(false) }

        if (!isLoggedIn) {
            LoginScreen(
                onLoginSuccess = {
                    isLoggedIn = true
                }
            )
        } else {
            HomeScreen()
        }
    }
}
