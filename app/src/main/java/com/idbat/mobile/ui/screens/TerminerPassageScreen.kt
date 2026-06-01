package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.SaisieMatiereLigne
import com.idbat.mobile.ui.theme.*
import com.idbat.mobile.ui.viewmodel.TerminerPassageViewModel
import com.idbat.mobile.ui.viewmodel.TerminerSaveState

@Composable
fun TerminerPassageScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    info: InfoCartePassage,
    lignes: List<SaisieMatiereLigne>,
    commentaire: String,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: TerminerPassageViewModel = hiltViewModel()
) {
    // RG5.3 : pré-rempli si email connu, vide sinon (RG5.1)
    var email by remember(info.contact) { mutableStateOf(info.contact ?: "") }

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState == TerminerSaveState.Saving

    LaunchedEffect(saveState) {
        if (saveState == TerminerSaveState.Success) {
            viewModel.resetState()
            onNavigateToHome()
        }
    }

    val hasExistingEmail = !info.contact.isNullOrBlank()
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
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bon de dépôt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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

            // Erreur éventuelle
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
                    if (!isSaving) {
                        // RG5.4 : email saisi/modifié → update local (sync BO à prévoir)
                        val emailToSave = email.trim().takeIf { it.isNotBlank() && it != info.contact }
                        viewModel.terminer(
                            contratId = contratId,
                            siteId = siteId,
                            carteId = info.carteId,
                            commentaire = commentaire,
                            lignes = lignes,
                            emailSaisi = email.trim().takeIf { it.isNotBlank() },
                            usagerId = info.usagerId
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
                    containerColor = MaterialTheme.colorScheme.onSurface
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
}
