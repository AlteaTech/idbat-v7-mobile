package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedSite: SiteEntity?,
    lastSynchroDateReception: Date?,
    lastSynchroDateEnvoi: Date?,
    onLogoutClick: () -> Unit,
    onTransferClick: () -> Unit,
    getSuiviContent: suspend (Long) -> String = { "" } // Changé le nom et le type de retour
) {
    val toastState = rememberToastState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF27059), Color(0xFFF5F5F5)),
                    startY = 0f,
                    endY = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Se déconnecter",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val lastSynchroDate = listOfNotNull(lastSynchroDateEnvoi, lastSynchroDateReception).maxOrNull()

            val formatter = SimpleDateFormat("dd/MM/yyyy 'à' HH'h'mm", Locale.FRANCE)
            val lastTransferText = if (lastSynchroDate != null) {
                "Dernier transfert le ${formatter.format(lastSynchroDate)}"
            } else {
                "Jamais synchronisé"
            }

            MainSiteCard(
                siteName = selectedSite?.nom ?: "Gennevilliers",
                lastTransfer = lastTransferText,
                lastSynchroDateReception = lastSynchroDateReception,
                lastSynchroDateEnvoi = lastSynchroDateEnvoi,
                onTransferClick = onTransferClick,
                onSuiviClick = {
                    val siteId = selectedSite?.id ?: 0L
                    coroutineScope.launch {
                        val suiviContent = getSuiviContent(siteId) // Appel asynchrone
                        toastState.showToast(
                            title = "Suivi des opérations",
                            content = suiviContent
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            ActionRowButton(
                title = "Saisie des signalements",
                onClick = {
                    toastState.showToast(
                        title = "Saisie des signalements",
                        content = "Module de signalement des incidents et anomalies constatés sur le terrain. Permet la création, modification et envoi de rapports détaillés vers le système central."
                    )
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            BottomLargeButton(
                title = "Passage en déchetterie",
                onClick = {
                    toastState.showToast(
                        title = "Passage en déchetterie",
                        content = "Gestion des passages et contrôles d'accès aux déchetteries. Inclut la vérification des autorisations, l'enregistrement des dépôts et le suivi des quotas."
                    )
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        ToastHost(toastState = toastState)
    }
}