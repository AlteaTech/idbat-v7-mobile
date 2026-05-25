package com.idbat.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.idbat.mobile.ui.theme.*
import kotlin.system.exitProcess

@Composable
fun ValidationErrorDialog() {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { /* Empêche la fermeture par clic à l'extérieur */ },
        properties = DialogProperties(
            dismissOnBackPress = false, // Empêche la fermeture avec le bouton retour
            dismissOnClickOutside = false, // Empêche la fermeture en cliquant à l'extérieur
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icône d'avertissement
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = VeoliaAlertOrange,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Titre
                Text(
                    text = "Validation requise",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message principal
                Text(
                    text = "Votre téléphone est en attente de validation",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message secondaire
                Text(
                    text = "Veuillez contacter votre administrateur pour activer ce terminal.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bouton OK
                Button(
                    onClick = {
                        // Fermer l'application
                        (context as? androidx.activity.ComponentActivity)?.finishAffinity()
                        exitProcess(0)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeoliaPrincipal
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}