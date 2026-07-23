package com.idbat.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.initializeApp() }

    LaunchedEffect(Unit) {
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocation) {
            viewModel.initializeApp()
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

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
       // TODO que faire ?
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppDestination.Login.route) {
            LoginScreen()
        }
        composable(AppDestination.Home.route) {
            HomeScreen(
                selectedSite = uiState.authState.loggedInSite,
                contratId = uiState.authState.loggedInContrat?.id ?: 0L,
                lastSynchroDateEnvoi = uiState.syncState.lastSynchroDateEnvoi,
                lastSynchroDateReception = uiState.syncState.lastSynchroDateReception,
                lastEnvoiSuccess = uiState.syncState.lastEnvoiSuccess,
                onTransferClick = { viewModel.executeTransfer() },
                isTransferring = uiState.syncState.isTransferring,
                getSuiviContent = { siteId -> viewModel.getSuiviContentAsync(siteId) },
                onNavigateToPoc = { navController.navigate(AppDestination.Poc.route) }
            )
        }
        composable(AppDestination.Poc.route) {
            PocScreen(onBack = { navController.popBackStack() })
        }
    }
}
