import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*

@Composable
fun App() {
    MaterialTheme {
        var isLoggedIn by remember { mutableStateOf(false) }

        if (isLoggedIn) {
            SuccessScreen()
        } else {
            LoginScreen(onLoginSuccess = { isLoggedIn = true })
        }
    }
}
