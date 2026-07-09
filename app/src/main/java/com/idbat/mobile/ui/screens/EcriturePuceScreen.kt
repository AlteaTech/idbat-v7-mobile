package com.idbat.mobile.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.model.CarteCreationQr
import com.idbat.mobile.ui.theme.*
import com.idbat.mobile.ui.viewmodel.EcriturePuceViewModel

@Composable
fun EcriturePuceScreen(
    siteName: String,
    carte: CarteCreationQr,
    contratId: Long,
    siteId: Long,
    onBack: () -> Unit,
    onFinished: () -> Unit = onBack,
    viewModel: EcriturePuceViewModel = hiltViewModel()
) {
    val bgColor = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val currentCarte = rememberUpdatedState(carte)

    // Reader mode actif tant qu'on n'a pas réussi l'écriture
    val isDone = state is EcriturePuceViewModel.WriteState.Success

    // Bip de succès à l'écriture validée
    LaunchedEffect(isDone) {
        if (isDone) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(isDone) {
        if (nfcAdapter == null || activity == null || isDone) return@DisposableEffect onDispose {}
        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                if (tag == null) return@enableReaderMode
                viewModel.write(tag, currentCarte.value, contratId, siteId)
            },
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
                    text = "Création de cartes",
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
                    is EcriturePuceViewModel.WriteState.Writing -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Écriture en cours…", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ne retirez pas la carte", fontSize = 14.sp, color = onSurface)
                    }

                    is EcriturePuceViewModel.WriteState.Error -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Nfc,
                            contentDescription = null,
                            tint = VeoliaErrorDark,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Échec de l'écriture", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VeoliaErrorDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(s.message, fontSize = 13.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.reset() },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = onSurface)
                        ) {
                            Text("Réessayer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Nfc,
                            contentDescription = null,
                            tint = onSurface,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Écriture RFID", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        val sousTexte = when {
                            nfcAdapter == null -> "NFC non disponible sur cet appareil"
                            !nfcAdapter.isEnabled -> "Activez le NFC dans les réglages"
                            else -> "Présentez votre carte RFID"
                        }
                        Text(sousTexte, fontSize = 14.sp, color = onSurface)
                    }
                }
            }
        }

        // Popup de confirmation après écriture + relecture conformes
        if (state is EcriturePuceViewModel.WriteState.Success) {
            CarteCreeeDialog(onFermer = {
                viewModel.reset()
                onFinished()
            })
        }
    }
}

@Composable
private fun CarteCreeeDialog(onFermer: () -> Unit) {
    Dialog(onDismissRequest = onFermer) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = VeoliaPrincipal,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Carte créée",
                    color = VeoliaPrincipal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cette carte sera utilisable à la prochaine synchronisation.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.horizontalGradient(listOf(VeoliaCoral, VeoliaCoralLight)))
                        .clickable { onFermer() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Fermer", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
