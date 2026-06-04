package com.idbat.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.ui.viewmodel.TestVolumeViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.R
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.ui.components.*
import com.idbat.mobile.ui.theme.VeoliaCoral
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    selectedSite: SiteEntity?,
    contrat: ContratEntity?,
    lastSynchroDateReception: Date?,
    lastSynchroDateEnvoi: Date?,
    lastEnvoiSuccess: Boolean?,
    onTransferClick: () -> Unit,
    onNavigateToPoc: () -> Unit,
    isTransferring: Boolean = false,
    getSuiviContent: suspend (Long) -> CharSequence = { "" },
    testVolumeViewModel: TestVolumeViewModel = hiltViewModel()
) {
    val toastState = rememberToastState()
    val coroutineScope = rememberCoroutineScope()
    var showCarteSheet by remember { mutableStateOf(false) }
    var showDepotScreen by remember { mutableStateOf(false) }

    val testVolumeState by testVolumeViewModel.state.collectAsStateWithLifecycle()

    when (val s = testVolumeState) {
        is TestVolumeViewModel.State.Running -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Test Volume Passage") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
        is TestVolumeViewModel.State.Done -> AlertDialog(
            onDismissRequest = { testVolumeViewModel.reset() },
            title = { Text("Terminé") },
            text = {
                Text(
                    "${s.totalPassages} passages créés\n" +
                    "(${s.totalSites} site(s) × ${s.totalUsers} utilisateur(s) × 1000)"
                )
            },
            confirmButton = {
                TextButton(onClick = { testVolumeViewModel.reset() }) { Text("OK") }
            }
        )
        is TestVolumeViewModel.State.Error -> AlertDialog(
            onDismissRequest = { testVolumeViewModel.reset() },
            title = { Text("Erreur") },
            text = { Text(s.message) },
            confirmButton = {
                TextButton(onClick = { testVolumeViewModel.reset() }) { Text("OK") }
            }
        )
        else -> Unit
    }

    if (showDepotScreen) {
        DepotScreen(
            siteName = selectedSite?.nom ?: "",
            siteId = selectedSite?.id ?: 0L,
            contrat = contrat,
            onBack = { showDepotScreen = false },
            onNavigateToHome = { showDepotScreen = false }
        )
        return
    }

    val lastSynchroDate = listOfNotNull(lastSynchroDateEnvoi, lastSynchroDateReception).maxOrNull()
    val formatter = SimpleDateFormat("dd/MM/yyyy 'à' HH'h'mm", Locale.FRANCE)
    val lastTransferText = if (lastSynchroDate != null) {
        "Dernier transfert le ${formatter.format(lastSynchroDate)}"
    } else {
        "Jamais synchronisé"
    }

    val bgColor = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to VeoliaCoral,
                        0.75f to bgColor,
                        1f to bgColor
                    )
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("id") }
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("bat") }
                    },
                    color = Color.White,
                    fontSize = 34.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            MainSiteCard(
                siteName = selectedSite?.nom ?: "",
                lastTransfer = lastTransferText,
                lastSynchroDateReception = lastSynchroDateReception,
                lastSynchroDateEnvoi = lastSynchroDateEnvoi,
                lastEnvoiSuccess = lastEnvoiSuccess,
                onTransferClick = onTransferClick,
                isTransferring = isTransferring,
                onSuiviClick = {
                    val siteId = selectedSite?.id ?: 0L
                    coroutineScope.launch {
                        val suiviContent = getSuiviContent(siteId)
                        toastState.showToast(
                            title = "Suivi des opérations",
                            content = suiviContent
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionRowButton(
                title = "Saisie des signalements",
                onClick = {
                    toastState.showToast(
                        title = "Saisie des signalements",
                        content = "Module de signalement des incidents et anomalies constatés sur le terrain. Permet la création, modification et envoi de rapports détaillés vers le système central."
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionRowButton(
                title = "Gestion des cartes",
                iconResId = R.drawable.carte_a_puce,
                onClick = { showCarteSheet = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            BottomLargeButton(
                title = "Passage en déchetterie",
                onClick = { showDepotScreen = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        ToastHost(toastState = toastState)

        if (showCarteSheet) {
            CarteActionSheet(
                onDismiss = { showCarteSheet = false },
                onPocClick = {
                    showCarteSheet = false
                    onNavigateToPoc()
                },
                onTestVolumeClick = { testVolumeViewModel.generer() }
            )
        }
    }
}
