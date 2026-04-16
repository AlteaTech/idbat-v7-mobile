package com.idbat.mobile

import android.os.Bundle
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.ui.screens.HomeScreen
import com.idbat.mobile.ui.screens.LoginScreen
import com.idbat.mobile.ui.theme.IdbatTheme
import com.idbat.mobile.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdbatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
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
                onTransferClick = { viewModel.executeTransfer() }
            )
        }
        
        else -> {
            LoginScreen(
                errorMessage = uiState.authState.loginError,
                availableSites = uiState.authState.availableSites,
                onLoginClick = { username, password, siteId ->
                    viewModel.login(username, password, siteId)
                }
            )
        }
    }
}