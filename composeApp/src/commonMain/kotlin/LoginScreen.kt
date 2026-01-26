import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LoginRequest(
    val login: String,
    val motDePasse: String
)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Configuration du client HTTP (à idéalement sortir dans un singleton ou DI)
    val client = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    fun tryLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Veuillez remplir tous les champs"
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                // Note: Sur l'émulateur Android, localhost pointe vers l'émulateur lui-même.
                // Pour accéder à la machine hôte, il faut utiliser 10.0.2.2
                // Si vous testez sur un vrai device, il faut l'IP locale de votre PC.
                val response: HttpResponse = client.post("http://10.0.2.2:8081/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(login = username, motDePasse = password))
                }

                if (response.status.isSuccess()) {
                    onLoginSuccess()
                } else {
                    errorMessage = "Échec de la connexion: ${response.status}"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connexion",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Identifiant") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colors.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { tryLogin() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colors.onPrimary
                )
            } else {
                Text("Se connecter")
            }
        }
    }
}
