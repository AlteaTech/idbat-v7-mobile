package com.idbat.mobile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.idbat.mobile.ui.navigation.AppDestination
import com.idbat.mobile.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.initializeApp() }

    if (!uiState.isInitialized) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = if (uiState.isLoggedIn) AppDestination.Home.route else AppDestination.Login.route

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(AppDestination.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(AppDestination.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val syncError = uiState.syncState.syncError
    if (syncError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSyncError() },
            title = { Text("Synchronisation impossible") },
            text = { Text(syncError) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSyncError() }) {
                    Text("OK")
                }
            }
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppDestination.Login.route) {
            LoginScreen()
        }
        composable(AppDestination.Home.route) {
            HomeScreen(
                selectedSite = uiState.authState.loggedInSite,
                contrat = uiState.authState.loggedInContrat,
                lastSynchroDateEnvoi = uiState.syncState.lastSynchroDateEnvoi,
                lastSynchroDateReception = uiState.syncState.lastSynchroDateReception,
                onTransferClick = { viewModel.executeTransfer() },
                getSuiviContent = { siteId -> viewModel.getSuiviContentAsync(siteId) },
                onNavigateToPoc = { navController.navigate(AppDestination.Poc.route) }
            )
        }
        composable(AppDestination.Poc.route) {
            PocScreen(onBack = { navController.popBackStack() })
        }
    }
}
