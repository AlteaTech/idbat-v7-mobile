package com.idbat.mobile.ui.screens

import android.net.Uri
import android.nfc.NfcAdapter
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.SaisieMatiereLigne
import com.idbat.mobile.ui.theme.VeoliaCoral
import com.idbat.mobile.ui.theme.VeoliaCoralLight
import com.idbat.mobile.ui.theme.VeoliaErrorDark
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.theme.VeoliaSubtle
import com.idbat.mobile.ui.theme.White
import com.idbat.mobile.ui.viewmodel.ContratViewModel
import com.idbat.mobile.ui.viewmodel.TerminerPassageViewModel
import com.idbat.mobile.ui.viewmodel.TerminerSaveState
import com.idbat.mobile.ui.viewmodel.TerminerWriteState

@Composable
fun TerminerPassageScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    info: InfoCartePassage,
    lignes: List<SaisieMatiereLigne>,
    commentaire: String,
    photos: List<Uri> = emptyList(),
    signatureImage: ImageBitmap? = null,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: TerminerPassageViewModel = hiltViewModel(),
    contratViewModel: ContratViewModel = hiltViewModel()
) {
    LaunchedEffect(contratId) { contratViewModel.setContratId(contratId) }
    val contrat by contratViewModel.contrat.collectAsStateWithLifecycle()

    // RG5.3 : pré-rempli si email connu, vide sinon (RG5.1)
    var email by remember(info.contact) { mutableStateOf(info.contact ?: "") }

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val writeState by viewModel.writeState.collectAsStateWithLifecycle()
    val isSaving = saveState == TerminerSaveState.Saving

    // RG1 : dépôt puce en pré-paiement ? (carte 'P' lue → info.uid non nul ; mode pré-paiement
    // selon le type d'apporteur). Dans ce cas, "Terminer" écrit d'abord le nouveau solde.
    val estPrepaiementPuce = info.uid != null && info.soldePoints != null && when (info.typeApporteurIsPro) {
        true  -> contrat?.hasPrepaiementProfessionnels == true
        false -> contrat?.hasPrepaiementParticuliers == true
        null  -> false
    }

    // RG2 : total des points = somme (quantité × tarif) — identique à l'écran de confirmation
    val totalPoints = lignes.sumOf { ligne ->
        (ligne.quantite.toDoubleOrNull() ?: 0.0) * ligne.matiere.tarif
    }
    val nouveauSolde = (info.soldePoints ?: 0.0) - totalPoints

    // Phase d'écriture NFC (RG3)
    var ecritureEnCours by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        if (saveState == TerminerSaveState.Success) {
            viewModel.resetState()
            onNavigateToHome()
        }
    }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    // Reader mode actif seulement pendant la phase d'écriture, tant que non réussie
    val readerActif = ecritureEnCours && writeState != TerminerWriteState.Success
    DisposableEffect(readerActif) {
        if (nfcAdapter == null || activity == null || !readerActif) return@DisposableEffect onDispose {}
        val callback = NfcAdapter.ReaderCallback { tag ->
            if (tag == null) return@ReaderCallback
            viewModel.ecrireSoldeEtTerminer(
                tag = tag,
                contratId = contratId,
                siteId = siteId,
                carteId = info.carteId,
                commentaire = commentaire,
                lignes = lignes,
                emailSaisi = email.trim().takeIf { it.isNotBlank() },
                usagerId = info.usagerId,
                photos = photos,
                signatureImage = signatureImage,
                uidCarteAttendu = info.uid ?: "",
                soldePointsAvant = info.soldePoints ?: 0.0,
                nouveauSolde = nouveauSolde
            )
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

    val hasExistingEmail = !info.contact.isNullOrBlank()
    val bgColor = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to VeoliaGradientTop,
                        0.35f to bgColor,
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
                    text = "Dépôt",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = siteName,
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (ecritureEnCours) {
                // RG3 : écran d'écriture du nouveau solde (repris du rechargement de carte)
                EcritureSoldeZone(
                    writeState = writeState,
                    onSurface = onSurface,
                    onRetry = { viewModel.resetWrite() }
                )
            } else {
                // Bloc "Bon de dépôt"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = onSurface
                            )
                            Text(
                                text = "Bon de dépôt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // RG5.1 / RG5.3
                        Text(
                            text = if (hasExistingEmail)
                                "Adresse e-mail pour envoyer le bon de dépôt"
                            else
                                "Pas d'adresse e-mail connue pour envoyer le bon de dépôt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // RG5.2 : facultatif — RG5.3 : modifiable
                        Text(
                            text = "Mail",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = VeoliaSubtle,
                                focusedBorderColor = VeoliaPrincipal
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Erreur éventuelle (enregistrement direct)
                if (saveState is TerminerSaveState.Error) {
                    Text(
                        text = (saveState as TerminerSaveState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Bouton "Terminer"
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        if (estPrepaiementPuce) {
                            // RG1 : ne pas enregistrer directement → écriture du nouveau solde
                            viewModel.resetWrite()
                            ecritureEnCours = true
                        } else {
                            viewModel.terminer(
                                contratId = contratId,
                                siteId = siteId,
                                carteId = info.carteId,
                                commentaire = commentaire,
                                lignes = lignes,
                                emailSaisi = email.trim().takeIf { it.isNotBlank() },
                                usagerId = info.usagerId,
                                photos = photos,
                                signatureImage = signatureImage,
                                uidCarte = info.uid,
                                soldePointsAvant = info.soldePoints
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(50.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onSurface
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSaving) "Enregistrement…" else "Terminer",
                        color = MaterialTheme.colorScheme.surface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // RG3.3 : confirmation après écriture + enregistrement réussis → retour accueil
        if (ecritureEnCours && writeState == TerminerWriteState.Success) {
            DepotConfirmDialog(onFermer = {
                viewModel.resetWrite()
                onNavigateToHome()
            })
        }
    }
}

@Composable
private fun ColumnScope.EcritureSoldeZone(
    writeState: TerminerWriteState,
    onSurface: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ConcentricCircles()

        when (writeState) {
            is TerminerWriteState.Writing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Écriture en cours…", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ne retirez pas la carte", fontSize = 14.sp, color = onSurface)
            }

            is TerminerWriteState.WrongCard -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Nfc, null, tint = VeoliaErrorDark, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ce n'est pas la même carte", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VeoliaErrorDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Présentez la carte lue au début du dépôt", fontSize = 13.sp, color = onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = onSurface)) {
                    Text("Réessayer", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                }
            }

            is TerminerWriteState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun DepotConfirmDialog(onFermer: () -> Unit) {
    Dialog(onDismissRequest = onFermer) {
        Surface(shape = RoundedCornerShape(20.dp), color = White) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = VeoliaPrincipal, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Dépôt enregistré", color = VeoliaPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Le nouveau solde a été écrit sur la carte.",
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
