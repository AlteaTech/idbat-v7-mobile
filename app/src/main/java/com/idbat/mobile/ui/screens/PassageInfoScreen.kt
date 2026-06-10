package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.entities.SeuilEtatEntity
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.ui.theme.VeoliaAlertOrange
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaLightGray
import com.idbat.mobile.ui.theme.White
import com.idbat.mobile.ui.viewmodel.PassageSaveState
import com.idbat.mobile.ui.viewmodel.PassageViewModel

@Composable
fun PassageInfoScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    info: InfoCartePassage,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onSaisirMatieres: () -> Unit = {},
    viewModel: PassageViewModel = hiltViewModel()
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState == PassageSaveState.Saving
    val seuils by viewModel.seuils.collectAsStateWithLifecycle()

    LaunchedEffect(info.usagerId) { viewModel.loadSeuils(info.usagerId) }

    LaunchedEffect(saveState) {
        if (saveState == PassageSaveState.Success) {
            viewModel.resetState()
            onNavigateToHome()
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

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Informations de la carte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!info.societe.isNullOrBlank()) {
                            InfoRow(label = "Société", value = info.societe)
                        }
                        InfoRow(label = "Nom du titulaire", value = info.nomTitulaire)
                        InfoRow(label = "N° de carte", value = info.numeroCarte)
                        InfoRow(label = "Type d'apporteur", value = info.typeApporteur)
                        InfoRow(label = "Contact", value = info.contact)
                    }
                }

                SeuilsCard(seuils = seuils)
            }

            // Usager bloqué : au moins un seuil atteint (isAlerte == false)
            val usagerBloque = seuils.any { !it.isAlerte }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (usagerBloque) {
                    Button(
                        onClick = onNavigateToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text(
                            text = "Fermer",
                            color = White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isSaving) viewModel.enregistrerPassage(contratId, siteId, info.carteId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VeoliaLightGray,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSaving) "Enregistrement…" else "Enregistrer le passage",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = onSaisirMatieres,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saisir les matières",
                            color = White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeuilsCard(seuils: List<SeuilEtatEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (seuils.isEmpty()) {
                Text(
                    text = "Aucune alerte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                seuils.forEach { seuil -> SeuilRow(seuil) }
            }
        }
    }
}

@Composable
private fun SeuilRow(seuil: SeuilEtatEntity) {
    val periode = if (seuil.seuilDetailPeriode.equals("annuel", ignoreCase = true)) " sur l'année" else ""
    val nbAutorises = seuil.seuilDetailNbPassage ?: seuil.nbPassagesAutorises
    val message = if (!seuil.isAlerte) {
        "Seuil ${seuil.nom} atteint, vous avez ${seuil.nbPassagesEffectues} passages$periode pour $nbAutorises autorisés"
    } else {
        "Le seuil ${seuil.nom} vous autorise $nbAutorises passages$periode, vous en avez déjà effectué ${seuil.nbPassagesEffectues}"
    }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = VeoliaAlertOrange,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
