package com.idbat.mobile.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.PassageSaveState
import com.idbat.mobile.data.model.SeuilEvalue
import com.idbat.mobile.ui.theme.VeoliaAlertOrange
import com.idbat.mobile.ui.theme.VeoliaErrorDark
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaLightGray
import com.idbat.mobile.ui.theme.White
import com.idbat.mobile.ui.viewmodel.ContratViewModel
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
    viewModel: PassageViewModel = hiltViewModel(),
    contratViewModel: ContratViewModel = hiltViewModel()
) {
    LaunchedEffect(contratId) { contratViewModel.setContratId(contratId) }
    val contrat by contratViewModel.contrat.collectAsStateWithLifecycle()

    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState == PassageSaveState.Saving
    val seuils by viewModel.seuils.collectAsStateWithLifecycle()
    val listeNoire by viewModel.listeNoire.collectAsStateWithLifecycle()

    // #513 : seuils évalués pour la carte présentée (unités de passage)
    LaunchedEffect(info.usagerId, info.carteId, contratId) {
        viewModel.loadSeuils(info.usagerId, contratId, info.carteId)
    }
    LaunchedEffect(info.carteId) { viewModel.loadListeNoire(info.carteId) }

    // Back système = back de l'écran (bouton haut-gauche)
    BackHandler { onBack() }

    // RG1 : un seul bouton selon le flag « accès simple » du contrat correspondant
    // au type d'apporteur de l'usager sélectionné. accessSimple == true → « Enregistrer
    // le passage » ; false → « Saisir les matières ». (isPro/contrat nuls = particulier/false.)
    val isPro = info.typeApporteurIsPro == true
    val accessSimple = if (isPro) {
        contrat?.hasAccessSimpleProfessionnels == true
    } else {
        contrat?.hasAccessSimpleParticuliers == true
    }

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
            val isListeNoire = listeNoire != null;
            // Passage bloqué : carte en liste noire OU un seuil BLOQUANT dont le plafond est atteint.
            // Sémantique back (UsagersMapper) : isAlerte = plafond NON atteint (true = sous le quota,
            // rechargements inclus) ; isBloquant = flag de config du seuil (bloque vs avertit seulement).
            // #487/#484 : un seuil atteint mais non bloquant n'empêche pas le passage (simple avertissement).
            // #513 : « atteint » = !isAlerte OU carte 'I' dont les unités dépassent les unités restantes.
            val usagerBloque = isListeNoire || seuils.any { it.bloque }

            // RG1/RG3 : dès que le refus s'affiche, on enregistre le passage refusé (une seule fois),
            // pour que l'info survive même si l'utilisateur kill l'app avant de fermer l'écran.
            var refusEnregistre by remember(info.carteId, info.usagerId) { mutableStateOf(false) }
            LaunchedEffect(usagerBloque) {
                if (usagerBloque && !refusEnregistre) {
                    refusEnregistre = true
                    viewModel.enregistrerPassageRefuse(
                        contratId = contratId,
                        siteId = siteId,
                        carteId = info.carteId,
                        commentaire = buildRefusComment(isListeNoire, seuils),
                        usagerId = info.usagerId,
                        uidCarte = info.uid,
                        emailUsager = info.contact
                    )
                }
            }
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
                        // Solde de points : uniquement pour une carte à puce (valeur lue sur la carte)
                        info.soldePoints?.let {
                            InfoRow(label = "Solde de points", value = "%.2f points".format(it))
                        }
                    }
                }

                // Liste noire : affichée au-dessus des seuils si la carte est blacklistée
                listeNoire?.let { ListeNoireCard(libelle = it.libelle) }

                // #513 : un seuil sous la prévention (isPreventionAtteinte = false) et non atteint
                // pour cette carte n'est pas une alerte → non affiché.
                SeuilsCard(seuils = seuils.filter { it.visible }, isListeNoire)
            }


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
                    if(accessSimple){
                        Button(
                            onClick = {
                                if (!isSaving) viewModel.enregistrerPassage(
                                    contratId, siteId, info.carteId,
                                    uidCarte = info.uid,
                                    soldePointsAvant = info.soldePoints,
                                    usagerId = info.usagerId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = White
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSaving) "Enregistrement…" else "Enregistrer le passage",
                                color = White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }else{
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
}

@Composable
private fun ListeNoireCard(libelle: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = VeoliaErrorDark,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Cette carte est en liste noire",
                    fontWeight = FontWeight.Bold,
                    color = VeoliaErrorDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SeuilsCard(seuils: List<SeuilEvalue>, usagerListeNoire: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        if (!usagerListeNoire) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!usagerListeNoire && seuils.isEmpty()) {
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
}

/**
 * RG3 : commentaire justifiant un passage refusé — reprend le(s) message(s) des seuils qui bloquent
 * réellement (atteint pour cette carte ET seuil bloquant, #513 inclus) + la liste noire.
 */
private fun buildRefusComment(isListeNoire: Boolean, seuils: List<SeuilEvalue>): String {
    val lignes = mutableListOf<String>()
    if (isListeNoire) lignes.add("Cette carte est en liste noire")
    seuils.filter { it.bloque }.forEach { lignes.add(it.message) }
    return lignes.joinToString("\n")
}

@Composable
private fun SeuilRow(seuilEvalue: SeuilEvalue) {
    // #487 : nombre autorisé = valeur back (rechargements inclus). #513 RG3.2 : libellé « atteint »
    // aussi quand le seuil n'est atteint qu'à cause des unités de la carte.
    val message = seuilEvalue.message
    // Orange = avertissement (zone de prévention) ; rouge = erreur (seuil atteint pour cette carte,
    // #513 inclus), même rouge que la liste noire. Texte gras de la couleur du picto.
    val couleur = if (seuilEvalue.atteint) VeoliaErrorDark else VeoliaAlertOrange
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = couleur,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = couleur
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
