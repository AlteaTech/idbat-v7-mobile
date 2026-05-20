package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.idbat.mobile.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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

    val lastSynchroDate = listOfNotNull(lastSynchroDateEnvoi, lastSynchroDateReception).maxOrNull()
    val formatter = SimpleDateFormat("dd/MM/yyyy 'à' HH'h'mm", Locale.FRANCE)
    val lastTransferText = if (lastSynchroDate != null) {
        "Dernier transfert le ${formatter.format(lastSynchroDate)}"
    } else {
        "Jamais synchronisé"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER ORANGE ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF8A282), Color(0xFFE96D71))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                }
            }

            // ── CONTENU (fond gris) ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionRowButton(
                    title = "Saisie des signalements",
                    onClick = {
                        toastState.showToast(
                            title = "Saisie des signalements",
                            content = "Module de signalement des incidents et anomalies constatés sur le terrain. Permet la création, modification et envoi de rapports détaillés vers le système central."
                        )
                    }
                )

                ActionRowButton(
                    title = "Gestion des cartes",
                    iconVector = Icons.Outlined.CreditCard,
                    onClick = {
                        toastState.showToast(
                            title = "Gestion des cartes",
                            content = "Gestion des cartes d'accès et badges RFID associés aux usagers et aux sites."
                        )
                    }
                )
            }

            // ── BOUTON BAS ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F4F4))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                BottomLargeButton(
                    title = "Passage en déchetterie",
                    onClick = {
                        toastState.showToast(
                            title = "Passage en déchetterie",
                            content = "Gestion des passages et contrôles d'accès aux déchetteries. Inclut la vérification des autorisations, l'enregistrement des dépôts et le suivi des quotas."
                        )
                    }
                )
            }
        }

        ToastHost(toastState = toastState)
    }
}
