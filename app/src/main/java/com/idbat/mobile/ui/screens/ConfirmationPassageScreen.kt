package com.idbat.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.SaisieMatiereLigne
import androidx.compose.ui.window.Dialog
import com.idbat.mobile.ui.components.PhotoPickerComponent
import com.idbat.mobile.ui.components.SignatureComponent
import com.idbat.mobile.ui.theme.*
import java.util.Locale

@Composable
fun ConfirmationPassageScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    contrat: ContratEntity?,
    info: InfoCartePassage,
    lignes: List<SaisieMatiereLigne>,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // États hoistés — déclarés avant les early returns pour survivre à la navigation enfant
    var commentaire by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var signatureImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showTerminer by remember { mutableStateOf(false) }

    // RG4 : visibilité du bloc signature
    val showSignatureBloc = when (info.typeApporteurIsPro) {
        false -> contrat?.hasSignatureParticuliers == true
        true  -> contrat?.hasSignatureProfessionnels == true
        null  -> false
    }

    // RG4.3 : Confirmer actif si pas de signature requise, ou si signature saisie
    val confirmerEnabled = !showSignatureBloc || signatureImage != null

    // RG1 : total = somme (quantite * tarif)
    val total = lignes.sumOf { ligne ->
        (ligne.quantite.toDoubleOrNull() ?: 0.0) * ligne.matiere.tarif
    }
    val totalFormate = String.format(Locale.FRANCE, "%.2f", total)

    if (showTerminer) {
        TerminerPassageScreen(
            siteName = siteName,
            siteId = siteId,
            contratId = contratId,
            info = info,
            lignes = lignes,
            commentaire = commentaire,
            photos = photos,
            signatureImage = signatureImage,
            onBack = { showTerminer = false },
            onNavigateToHome = onNavigateToHome
        )
        return
    }

    if (showSignatureDialog) {
        Dialog(onDismissRequest = { showSignatureDialog = false }) {
            SignatureComponent(
                onValidate = { bitmap ->
                    signatureImage = bitmap
                    showSignatureDialog = false
                },
                onCancel = { showSignatureDialog = false }
            )
        }
    }

    val bgColor = MaterialTheme.colorScheme.background

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
                    text = "Dépôt",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = siteName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bloc Total (RG1)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = VeoliaCoral),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$totalFormate points (€)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }
                    }
                }

                // Bloc Commentaire (RG2)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Commentaire",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = commentaire,
                                // RG2.2 : max 50 caractères
                                onValueChange = { if (it.length <= 50) commentaire = it },
                                placeholder = {
                                    Text(
                                        text = "Écrivez votre commentaire ici",
                                        color = VeoliaPlaceholder
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = VeoliaSubtle,
                                    focusedBorderColor = VeoliaPrincipal
                                )
                            )
                            Text(
                                text = "${commentaire.length}/50",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Bloc Photo (RG3 : facultatif)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Photo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            PhotoPickerComponent(
                                photos = photos,
                                onPhotosChange = { photos = it }
                            )
                        }
                    }
                }

                // Bloc Signature (RG4 : conditionnel)
                if (showSignatureBloc) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                        imageVector = Icons.Default.Draw,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Signature",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (signatureImage != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = signatureImage!!,
                                        contentDescription = "Signature",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .border(1.dp, VeoliaSubtle, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { signatureImage = null }) {
                                        Text("Recommencer", color = VeoliaPrincipal)
                                    }
                                } else {
                                    Button(
                                        onClick = { showSignatureDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Draw,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Signer", color = White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // RG4.3 : Confirmer actif seulement si signature présente (ou pas requise)
            Button(
                onClick = { showTerminer = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                enabled = confirmerEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = VeoliaDisabled
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (confirmerEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmer",
                    color = if (confirmerEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
