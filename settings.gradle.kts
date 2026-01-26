rootProject.name = "idbat-v7-mobile"
include(":composeApp")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        val kotlinVersion = extra["plugin.kotlin"] as String
        val androidAppVersion = extra["plugin.androidApp"] as String
        val androidLibVersion = extra["plugin.androidLib"] as String
        val composeVersion = extra["plugin.compose"] as String

        id("com.android.application") version androidAppVersion
        id("com.android.library") version androidLibVersion
        id("org.jetbrains.kotlin.multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
