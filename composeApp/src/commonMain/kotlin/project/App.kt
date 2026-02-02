package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.screens.HomeScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        HomeScreen()
    }
}
