package org.example.project.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.networking.AuthService
import org.example.project.networking.LoginRequest

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val authService = remember { AuthService() }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connexion",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextField(
            value = username,
            onValueChange = { 
                username = it
                errorMessage = null 
            },
            label = { Text("Nom d'utilisateur") },
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null
            },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { 
                    isLoading = true
                    errorMessage = null
                    
                    scope.launch {
                        val result = authService.login(LoginRequest(username, password))
                        isLoading = false
                        
                        result.onSuccess { response ->
                            // Ici, vous pourriez sauvegarder le token (ex: Settings)
                            println("Token reçu: ${response.token}")
                            onLoginSuccess()
                        }.onFailure { error ->
                            errorMessage = error.message ?: "Une erreur est survenue"
                        }
                    }
                },
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Se connecter")
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
