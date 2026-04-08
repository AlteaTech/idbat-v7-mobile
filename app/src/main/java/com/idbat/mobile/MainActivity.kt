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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.idbat.mobile.ui.screens.LoginScreen
import com.idbat.mobile.ui.screens.SuccessScreen
import com.idbat.mobile.ui.theme.IdbatTheme

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

                    if (isLoggedIn) {
                        SuccessScreen()
                    } else {
                        LoginScreen(onLoginClick = { username, password ->
                            // Ici on simule une connexion réussie
                            // TODO: Remplacer par un vrai appel API
                            if (username.isNotBlank() && password.isNotBlank()) {
                                isLoggedIn = true
                            }
                        })
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
        LoginScreen(onLoginClick = { _, _ -> })
    }
}
