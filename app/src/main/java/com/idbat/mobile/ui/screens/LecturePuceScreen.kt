package com.idbat.mobile.ui.screens

import android.nfc.NfcAdapter
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.ui.theme.VeoliaErrorDark
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.White
import com.idbat.mobile.ui.viewmodel.LecturePuceViewModel

@Composable
fun LecturePuceScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: LecturePuceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Carte lue + usager trouvé → parcours passage identique au flux "Autres"
    var showSaisieMatiere by remember { mutableStateOf(false) }
    (state as? LecturePuceViewModel.ReadState.Ready)?.let { ready ->
        val info = ready.info
        if (showSaisieMatiere) {
            SaisieMatiereScreen(
                siteName = siteName,
                siteId = siteId,
                contratId = contratId,
                info = info,
                onBack = { showSaisieMatiere = false },
                onNavigateToHome = {
                    showSaisieMatiere = false
                    viewModel.reset()
                    onNavigateToHome()
                }
            )
            return
        }
        PassageInfoScreen(
            siteName = siteName,
            siteId = siteId,
            contratId = contratId,
            info = info,
            onBack = { viewModel.reset() },   // retour → relecture d'une carte
            onNavigateToHome = {
                viewModel.reset()
                onNavigateToHome()
            },
            onSaisirMatieres = { showSaisieMatiere = true }
        )
        return
    }

    // Back système = back de l'écran (bouton haut-gauche)
    BackHandler { onBack() }

    val bgColor = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    // Reader mode actif tant qu'on n'a pas un résultat final (Ready / NotRecognized)
    val stopReader = state is LecturePuceViewModel.ReadState.NotRecognized
    DisposableEffect(stopReader) {
        if (nfcAdapter == null || activity == null || stopReader) return@DisposableEffect onDispose {}
        nfcAdapter.enableReaderMode(
            activity,
            { tag -> if (tag != null) viewModel.read(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
        onDispose {
            try { nfcAdapter.disableReaderMode(activity) } catch (_: Exception) {}
        }
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
                    tint = onSurface
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text(
                    text = "Dépôt",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = siteName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                )
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val base = size.width * 0.18f
                    listOf(1f, 1.9f, 2.8f, 3.7f).forEach { factor ->
                        drawCircle(
                            color = White.copy(alpha = 0.35f),
                            radius = base * factor,
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                when (val s = state) {
                    is LecturePuceViewModel.ReadState.Reading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lecture en cours…", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ne retirez pas la carte", fontSize = 14.sp, color = onSurface)
                    }

                    is LecturePuceViewModel.ReadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Nfc, null, tint = VeoliaErrorDark, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Échec de la lecture", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VeoliaErrorDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(s.message, fontSize = 13.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.reset() }, shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = onSurface)) {
                            Text("Réessayer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Nfc, null, tint = onSurface, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lecture de la carte à puce", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        val sousTexte = when {
                            nfcAdapter == null -> "NFC non disponible sur cet appareil"
                            !nfcAdapter.isEnabled -> "Activez le NFC dans les réglages"
                            else -> "Présentez votre carte à puce"
                        }
                        Text(sousTexte, fontSize = 14.sp, color = onSurface)
                    }
                }
            }
        }

        // Carte non reconnue → popup ; à la fermeture, retour au choix (Autres / Carte à puce)
        if (state is LecturePuceViewModel.ReadState.NotRecognized) {
            AlertDialog(
                onDismissRequest = onBack,
                text = { Text("Carte non reconnue") },
                confirmButton = {
                    TextButton(onClick = { viewModel.reset(); onBack() }) { Text("OK") }
                }
            )
        }
    }
}
