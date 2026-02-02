package org.example.project.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 1. Modèles de données (Request / Response)
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String? = null
)

// 2. Service d'Authentification
class AuthService {
    // Configuration du client HTTP avec support JSON
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // URL de l'API (À adapter selon votre environnement : localhost, IP locale, ou URL de prod)
    // Note: Pour Android Emulator, localhost est 10.0.2.2
    // Pour iOS Simulator, localhost est 127.0.0.1
    // Il faudra idéalement gérer cela via la config
    private val baseUrl = "https://votre-api.com/api" 

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            // Simulation d'appel API pour l'exemple (à remplacer par le vrai appel ci-dessous)
            // val response: LoginResponse = client.post("$baseUrl/auth/login") {
            //     contentType(ContentType.Application.Json)
            //     setBody(request)
            // }.body()
            
            // Simulation d'un délai réseau
            kotlinx.coroutines.delay(1000)
            
            if (request.username.isNotEmpty() && request.password.isNotEmpty()) {
                Result.success(LoginResponse(token = "fake-jwt-token", userId = "123"))
            } else {
                Result.failure(Exception("Nom d'utilisateur ou mot de passe incorrect"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
