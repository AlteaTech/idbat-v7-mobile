package com.idbat.mobile.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.ui.viewmodel.MainViewModel


@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.initializeApp()
    }

    when {
        !uiState.isInitialized -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.isLoggedIn -> {
            HomeScreen(
                selectedSite = uiState.authState.loggedInSite,
                lastSynchroDateEnvoi = uiState.syncState.lastSynchroDateEnvoi,
                lastSynchroDateReception = uiState.syncState.lastSynchroDateReception,
                onLogoutClick = { viewModel.logout() },
                onTransferClick = { viewModel.executeTransfer() },
                getSuiviContent = { siteId ->
                    viewModel.getSuiviContentAsync(siteId)


                }
            )
        }

        else -> {
            LoginScreen(
                errorMessage = uiState.authState.loginError,
                availableSites = uiState.authState.availableSites,
                availableUsers = uiState.authState.availableUtilisateursTps,
                onLoginClick = { username, password, siteId ->
                    viewModel.login(username, password, siteId)
                }
            )
        }
    }
}