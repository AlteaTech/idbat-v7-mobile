package com.idbat.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.R
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaSubtle
import com.idbat.mobile.ui.theme.White

@Composable
fun DepotScreen(
    siteName: String,
    contrat: ContratEntity?,
    onBack: () -> Unit
) {
    var showAutresScreen by remember { mutableStateOf(false) }

    if (showAutresScreen) {
        AutresCartesScreen(
            siteName = siteName,
            contrat = contrat,
            onBack = { showAutresScreen = false }
        )
        return
    }

    val bgColor = MaterialTheme.colorScheme.background
    val showCarteAPuce = contrat?.hasPuce == true
    val showAutres = contrat?.hasCodebarres == true
            || contrat?.hasImmatriculation == true
            || contrat?.hasSelectionusager == true
    val iconTint = ColorFilter.tint(MaterialTheme.colorScheme.onSurface, BlendMode.SrcIn)

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

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Type de carte",
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showCarteAPuce) {
                            TypeCarteButton(
                                label = "Carte à puce",
                                sousTexte = "Cliquez avant de présenter la carte",
                                iconResId = R.drawable.carte_a_puce,
                                iconTint = iconTint,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { }
                            )
                        }
                        if (showAutres) {
                            TypeCarteButton(
                                label = "Autres",
                                sousTexte = null,
                                iconResId = R.drawable.autres_cartes_2,
                                iconTint = null,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showAutresScreen = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeCarteButton(
    label: String,
    sousTexte: String?,
    iconResId: Int,
    iconTint: ColorFilter?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VeoliaSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(width = 91.dp, height = 59.dp),
            colorFilter = iconTint
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (sousTexte != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sousTexte,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
