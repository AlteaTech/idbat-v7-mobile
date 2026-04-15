package com.idbat.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.idbat.mobile.ui.screens.LoginScreen
import com.idbat.mobile.ui.screens.HomeScreen
import com.idbat.mobile.ui.viewmodel.HomeViewModel
import com.idbat.mobile.ui.viewmodel.LoginViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            if (uiState.isLoginSuccessful) {
                // Si la connexion a réussi, on navigue vers l'écran d'accueil
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                // On réinitialise l'état pour que si on se déconnecte, on ne soit pas redirigé immédiatement
                viewModel.resetState()
            }

            LoginScreen(
                errorMessage = uiState.errorMessage,
                availableSites = uiState.availableSites,
                onLoginClick = { username, password, siteId ->
                    viewModel.login(username, password, siteId)
                }
            )
        }
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            HomeScreen(
                selectedSite = uiState.selectedSite,
                lastSynchroDateEnvoi = uiState.lastSynchroEnvoi,
                lastSynchroDateReception = uiState.lastSynchroReception,
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onTransferClick = {
                    uiState.selectedSite?.let { site ->
                        viewModel.recordTransfer(site.id)
                    }
                }
            )
        }
    }
}
