package com.idbat.mobile.ui.screens

import android.nfc.NfcAdapter
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.model.RechargeCarteInfo
import com.idbat.mobile.ui.theme.VeoliaCoral
import com.idbat.mobile.ui.theme.VeoliaCoralLight
import com.idbat.mobile.ui.theme.VeoliaErrorDark
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.theme.VeoliaSubtle
import com.idbat.mobile.ui.theme.White
import com.idbat.mobile.ui.viewmodel.RechargeCarteViewModel

@Composable
fun RechargeCarteScreen(
    siteName: String,
    contratId: Long,
    siteId: Long,
    onBack: () -> Unit,
    viewModel: RechargeCarteViewModel = hiltViewModel()
) {
    val bgColor = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val state by viewModel.state.collectAsStateWithLifecycle()
    val writeState by viewModel.writeState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    // Phase d'écriture (RG3) : déclenchée par "Soumettre" du formulaire
    var ecritureEnCours by remember { mutableStateOf(false) }
    var pointsToWrite by remember { mutableStateOf(0) }

    // Quelle lecture NFC est active : READ (lecture initiale) / WRITE (réécriture) / NONE
    val phase = when {
        ecritureEnCours && writeState !is RechargeCarteViewModel.WriteState.Success -> "WRITE"
        !ecritureEnCours && state is RechargeCarteViewModel.ReadState.Ready -> "NONE"
        !ecritureEnCours && state !is RechargeCarteViewModel.ReadState.NotEligible -> "READ"
        else -> "NONE"
    }
    DisposableEffect(phase) {
        if (nfcAdapter == null || activity == null || phase == "NONE") return@DisposableEffect onDispose {}
        val callback = NfcAdapter.ReaderCallback { tag ->
            if (tag == null) return@ReaderCallback
            if (phase == "READ") viewModel.read(tag, contratId)
            else viewModel.ecrireRechargement(tag, pointsToWrite, contratId, siteId)
        }
        nfcAdapter.enableReaderMode(
            activity,
            callback,
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
                onClick = {
                    if (ecritureEnCours) { ecritureEnCours = false; viewModel.resetWrite() }
                    else onBack()
                },
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
                    text = "Recharger une carte",
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

            when {
                ecritureEnCours ->
                    EcritureZone(writeState = writeState, onSurface = onSurface, onRetry = { viewModel.resetWrite() })

                state is RechargeCarteViewModel.ReadState.Ready ->
                    RechargeForm(
                        info = (state as RechargeCarteViewModel.ReadState.Ready).info,
                        onSubmit = { pts ->
                            pointsToWrite = pts
                            viewModel.resetWrite()
                            ecritureEnCours = true
                        }
                    )

                else ->
                    ReadStatusZone(state = state, nfcAdapter = nfcAdapter, onSurface = onSurface, onRetry = { viewModel.reset() })
            }
        }

        // RG2c : carte non éligible → message puis sortie
        if (state is RechargeCarteViewModel.ReadState.NotEligible) {
            AlertDialog(
                onDismissRequest = onBack,
                title = { Text("Carte non éligible") },
                text = { Text("Cette carte n'est pas éligible au pré-paiement") },
                confirmButton = { TextButton(onClick = onBack) { Text("Fermer") } }
            )
        }

        // RG3.3 : confirmation après écriture réussie → retour accueil
        if (ecritureEnCours && writeState is RechargeCarteViewModel.WriteState.Success) {
            RechargeConfirmDialog(onFermer = {
                viewModel.reset()
                onBack()
            })
        }
    }
}

@Composable
private fun ColumnScope.EcritureZone(
    writeState: RechargeCarteViewModel.WriteState,
    onSurface: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ConcentricCircles()

        when (writeState) {
            is RechargeCarteViewModel.WriteState.Writing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Écriture en cours…", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ne retirez pas la carte", fontSize = 14.sp, color = onSurface)
            }

            is RechargeCarteViewModel.WriteState.WrongCard -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Nfc, null, tint = VeoliaErrorDark, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ce n'est pas la même carte", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VeoliaErrorDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Présentez la carte qui vient d'être lue", fontSize = 13.sp, color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = onSurface)) {
                    Text("Réessayer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                }
            }

            is RechargeCarteViewModel.WriteState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Nfc, null, tint = VeoliaErrorDark, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Échec de l'écriture", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VeoliaErrorDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(writeState.message, fontSize = 13.sp, color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = onSurface)) {
                    Text("Réessayer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                }
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Nfc, null, tint = onSurface, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Écriture du nouveau solde", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Présentez votre carte RFID", fontSize = 14.sp, color = onSurface)
            }
        }
    }
}

@Composable
private fun ColumnScope.ReadStatusZone(
    state: RechargeCarteViewModel.ReadState,
    nfcAdapter: NfcAdapter?,
    onSurface: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ConcentricCircles()

        when (state) {
            is RechargeCarteViewModel.ReadState.Reading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Lecture en cours…", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ne retirez pas la carte", fontSize = 14.sp, color = onSurface)
            }

            is RechargeCarteViewModel.ReadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Nfc, null, tint = VeoliaErrorDark, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Échec de la lecture", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VeoliaErrorDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(state.message, fontSize = 13.sp, color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = onSurface)) {
                    Text("Réessayer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                }
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Nfc, null, tint = onSurface, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Lecture de la carte RFID", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurface)
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

@Composable
private fun ConcentricCircles() {
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
}

@Composable
private fun ColumnScope.RechargeForm(
    info: RechargeCarteInfo,
    onSubmit: (points: Int) -> Unit
) {
    var points by remember { mutableStateOf("") }
    val pointsInt = points.toIntOrNull() ?: 0
    val futurSolde = info.soldePoints + pointsInt

    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Bloc 1 : Informations de la carte ──────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Text("Informations de la carte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (info.typeApporteurIsPro && !info.societe.isNullOrBlank()) {
                    InfoRow(label = "Société", value = info.societe)
                }
                InfoRow(label = "Nom du titulaire", value = info.nomTitulaire)
                InfoRow(label = "N° de carte", value = info.numeroCarte)
                InfoRow(label = "Type d'apporteur", value = info.typeApporteur)
                InfoRow(label = "Contact", value = info.contact)
                InfoRow(label = "Solde de points", value = info.soldePoints.toString())
            }
        }

        // ── Bloc 2 : Rechargement ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Text("Rechargement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Nombre de points à recharger", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = points,
                        onValueChange = { input -> points = input.filter { it.isDigit() }.take(3) },
                        placeholder = { Text("Ex : 50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = VeoliaSubtle,
                            focusedBorderColor = VeoliaPrincipal
                        )
                    )
                    Text("Points", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    if (pointsInt > 0) {
                        Text("soit $pointsInt €", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Calcul du futur solde", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$futurSolde points", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // ── Bloc 3 : Soumettre ─────────────────────────────────────────────────────
    Button(
        onClick = { onSubmit(pointsInt) },
        enabled = pointsInt > 0,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Soumettre", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun RechargeConfirmDialog(onFermer: () -> Unit) {
    Dialog(onDismissRequest = onFermer) {
        Surface(shape = RoundedCornerShape(20.dp), color = White) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CreditCard, null, tint = VeoliaPrincipal, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Carte rechargée", color = VeoliaPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Le nouveau solde a été écrit sur la carte.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

@Composable
private fun InfoRow(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value?.ifBlank { "-" } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
