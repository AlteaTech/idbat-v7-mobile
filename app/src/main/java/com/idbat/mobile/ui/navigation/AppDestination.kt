package com.idbat.mobile.ui.navigation

sealed class AppDestination(val route: String) {
    object Login : AppDestination("login")
    object Home  : AppDestination("home")
    object Poc   : AppDestination("poc")
}
