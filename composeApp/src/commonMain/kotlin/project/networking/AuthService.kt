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
import org.example.project.BuildKonfig

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

    // URL de l'API injectée depuis la configuration de build
    private val baseUrl = BuildKonfig.BASE_URL

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response: LoginResponse = client.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (request.username.isNotEmpty() && request.password.isNotEmpty()) {
                Result.success(response)
            } else {
                Result.failure(Exception("Nom d'utilisateur ou mot de passe incorrect"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
