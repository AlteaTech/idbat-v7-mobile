package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.idbat.mobile.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    selectedSite: SiteEntity?,
    lastSynchroDateReception: Date?,
    lastSynchroDateEnvoi: Date?,
    onLogoutClick: () -> Unit,
    onTransferClick: () -> Unit,
    getSuiviContent: suspend (Long) -> CharSequence = { "" }
) {
    val toastState = rememberToastState()
    val coroutineScope = rememberCoroutineScope()
    var showCarteSheet by remember { mutableStateOf(false) }
    var showPocScreen by remember { mutableStateOf(false) }

    if (showPocScreen) {
        PocScreen(onBack = { showPocScreen = false })
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
                        0f to Color(0xFFF27059),
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
                onTransferClick = onTransferClick,
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
                onClick = {
                    toastState.showToast(
                        title = "Passage en déchetterie",
                        content = "Gestion des passages et contrôles d'accès aux déchetteries. Inclut la vérification des autorisations, l'enregistrement des dépôts et le suivi des quotas."
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        ToastHost(toastState = toastState)

        if (showCarteSheet) {
            CarteActionSheet(
                onDismiss = { showCarteSheet = false },
                onPocClick = { showPocScreen = true }
            )
        }
    }
}
