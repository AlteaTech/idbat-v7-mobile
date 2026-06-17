package com.idbat.mobile.ui.screens

import android.nfc.NfcAdapter
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
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
import com.idbat.mobile.data.model.CarteCreationQr
import com.idbat.mobile.ui.theme.VeoliaErrorDark
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaSuccess
import com.idbat.mobile.ui.theme.White
import com.idbat.mobile.ui.viewmodel.EcriturePuceViewModel

@Composable
fun EcriturePuceScreen(
    siteName: String,
    carte: CarteCreationQr,
    onBack: () -> Unit,
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
    DisposableEffect(isDone) {
        if (nfcAdapter == null || activity == null || isDone) return@DisposableEffect onDispose {}
        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                if (tag == null) return@enableReaderMode
                val uid = tag.id.joinToString(" ") { "%02X".format(it) }
                val numSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }
                viewModel.write(tag, currentCarte.value.toCartePuce(uid, numSerie))
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

                    is EcriturePuceViewModel.WriteState.Success -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = VeoliaSuccess,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Carte écrite", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = onSurface)
                        ) {
                            Text("Terminer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                        }
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
                        Text("Présentez votre carte RFID", fontSize = 14.sp, color = onSurface)
                    }
                }
            }
        }
    }
}
