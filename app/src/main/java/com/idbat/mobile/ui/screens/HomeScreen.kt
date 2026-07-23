package com.idbat.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.R
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.ui.components.*
import com.idbat.mobile.ui.theme.VeoliaCoral
import com.idbat.mobile.utils.FileLogger
import com.idbat.mobile.ui.viewmodel.ContratViewModel
import com.idbat.mobile.ui.viewmodel.TestVolumeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    selectedSite: SiteEntity?,
    contratId: Long,
    lastSynchroDateReception: Date?,
    lastSynchroDateEnvoi: Date?,
    lastEnvoiSuccess: Boolean?,
    onTransferClick: () -> Unit,
    onNavigateToPoc: () -> Unit,
    isTransferring: Boolean = false,
    getSuiviContent: suspend (Long) -> CharSequence = { "" },
    testVolumeViewModel: TestVolumeViewModel = hiltViewModel(),
    contratViewModel: ContratViewModel = hiltViewModel()
) {
    LaunchedEffect(contratId) { contratViewModel.setContratId(contratId) }
    val contrat by contratViewModel.contrat.collectAsStateWithLifecycle()

    val toastState = rememberToastState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    // Horodatages des derniers clics sur le logo (easter egg export logs)
    var logoClicks by remember { mutableStateOf<List<Long>>(emptyList()) }
    var showCarteSheet by remember { mutableStateOf(false) }
    var showDepotScreen by remember { mutableStateOf(false) }
    var showSignalementScreen by remember { mutableStateOf(false) }
    var showCreationCarteScreen by remember { mutableStateOf(false) }
    var showRechargeCarteScreen by remember { mutableStateOf(false) }

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
            contratId = contratId,
            onBack = { showDepotScreen = false },
            onNavigateToHome = { showDepotScreen = false }
        )
        return
    }

    if (showSignalementScreen) {
        SaisieSignalementScreen(
            siteName = selectedSite?.nom ?: "",
            siteId = selectedSite?.id ?: 0L,
            contratId = contratId,
            onBack = { showSignalementScreen = false }
        )
        return
    }

    if (showCreationCarteScreen) {
        CreationCarteScreen(
            siteName = selectedSite?.nom ?: "",
            contratId = contratId,
            siteId = selectedSite?.id ?: 0L,
            onBack = { showCreationCarteScreen = false }
        )
        return
    }

    if (showRechargeCarteScreen) {
        RechargeCarteScreen(
            siteName = selectedSite?.nom ?: "",
            contratId = contratId,
            siteId = selectedSite?.id ?: 0L,
            onBack = { showRechargeCarteScreen = false }
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

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "idbat by Veolia",
                modifier = Modifier
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 4 clics rapides (< 1,2 s glissant) → export des logs par e-mail
                        val now = System.currentTimeMillis()
                        logoClicks = (logoClicks + now).filter { now - it <= 2000 }
                        if (logoClicks.size >= 4) {
                            logoClicks = emptyList()
                            if (!FileLogger.shareLogsByEmail(context)) {
                                toastState.showToast("Logs", "Aucun log à envoyer")
                            }
                        }
                    }
            )

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
                onClick = { showSignalementScreen = true }
            )

            if (contrat?.hasPuce == true) {
                Spacer(modifier = Modifier.height(12.dp))

                ActionRowButton(
                    title = "Gestion des cartes",
                    iconResId = R.drawable.carte_a_puce,
                    onClick = { showCarteSheet = true }
                )
            }

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
                onCreerCarte = {
                    showCarteSheet = false
                    showCreationCarteScreen = true
                },
                onRechargerCarte = {
                    showCarteSheet = false
                    showRechargeCarteScreen = true
                },
                onPocClick = {
                    showCarteSheet = false
                    onNavigateToPoc()
                },
                onTestVolumeClick = { testVolumeViewModel.generer() },
                // RG0 : "Recharger" visible si puce + (prépaiement particuliers OU professionnels)
                showRecharger = contrat?.hasPuce == true &&
                        (contrat?.hasPrepaiementParticuliers ?: false || contrat?.hasPrepaiementProfessionnels ?: false)
            )
        }
    }
}
