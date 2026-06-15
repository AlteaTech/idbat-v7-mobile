package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idbat.mobile.data.model.CarteCreationQr
import com.idbat.mobile.ui.components.BarcodeScannerComponent
import com.idbat.mobile.ui.theme.VeoliaErrorDark
import com.idbat.mobile.ui.theme.VeoliaGradientTop

@Composable
fun CreationCarteScreen(
    siteName: String,
    onBack: () -> Unit
) {
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var carteParsee by remember { mutableStateOf<CarteCreationQr?>(null) }
    var formatError by remember { mutableStateOf(false) }
    val bgColor = MaterialTheme.colorScheme.background

    // Une fois la carte parsée, on bascule sur l'écran d'informations
    carteParsee?.let { carte ->
        CarteCreationInfoScreen(
            siteName = siteName,
            carte = carte,
            onBack = {
                carteParsee = null
                scannedValue = null
            },
            onValider = onBack
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to VeoliaGradientTop,
                        0.30f to bgColor,
                        1f to bgColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text(
                    text = "Création de cartes",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = siteName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BarcodeScannerComponent(
                modifier = Modifier.padding(horizontal = 24.dp),
                title = "QR code pour la création de la carte à puce",
                subtitle = if (formatError) "QR code invalide, réessayez" else "Scannez le code",
                scannedValue = scannedValue,
                onBarcodeDetected = { value, _ ->
                    scannedValue = value
                    val carte = CarteCreationQr.parse(value)
                    if (carte != null) {
                        formatError = false
                        carteParsee = carte
                    } else {
                        formatError = true
                        scannedValue = null  // réarme le scanner
                    }
                }
            )

            if (formatError) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Le QR code scanné ne respecte pas le format attendu (${CarteCreationQr.TRAME_LENGTH} caractères).",
                    color = VeoliaErrorDark,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
