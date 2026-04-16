package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.data.entities.SiteEntity
import java.text.SimpleDateFormat
import java.util.*
import com.idbat.mobile.R
import com.idbat.mobile.data.AppDatabase
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.components.*

@Composable
fun MainSiteCard(
    siteName: String,
    lastTransfer: String,
    lastSynchroDateReception: Date?,
    lastSynchroDateEnvoi: Date?,
    onTransferClick: () -> Unit,
    onSuiviClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = siteName,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            Text(
                text = lastTransfer,
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(text = "Envoi", isSuccess = lastSynchroDateEnvoi != null)
                StatusBadge(text = "Réception", isSuccess = lastSynchroDateReception != null)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSuiviClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Suivi", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Transférer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
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