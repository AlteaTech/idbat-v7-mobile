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
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.entities.MotifAutorisationContratEntity
import com.idbat.mobile.data.model.PassageInconnuInfo
import com.idbat.mobile.data.model.PassageSaveState
import com.idbat.mobile.ui.theme.*
import com.idbat.mobile.ui.viewmodel.PassageInconnuViewModel

private const val MESSAGE_MAX_ATTEINT = "Nombre de passages exceptionnels autorisés atteint"

/** "s" du pluriel français : uniquement à partir de 2 (0 et 1 restent au singulier). */
private fun pluriel(n: Int): String = if (n >= 2) "s" else ""

/**
 * #533 : passage d'une immatriculation inconnue (contrat avec `nbPassagesInconnusAcceptes > 0`).
 * Contrôle d'accès seul (RG1.5) : pas de titulaire / apporteur / contact, ni liste noire ni seuils.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageInconnuScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    info: PassageInconnuInfo,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: PassageInconnuViewModel = hiltViewModel()
) {
    LaunchedEffect(contratId) { viewModel.setContratId(contratId) }
    val motifs by viewModel.motifs.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val isSaving = saveState == PassageSaveState.Saving

    // RG2.1 : motif obligatoire au 1er passage
    var motifSelectionne by remember(info.immatriculation) { mutableStateOf<MotifAutorisationContratEntity?>(null) }
    var showMotifObligatoire by remember { mutableStateOf(false) }

    // Back système = back de l'écran (bouton haut-gauche)
    BackHandler { onBack() }

    LaunchedEffect(saveState) {
        if (saveState == PassageSaveState.Success) {
            viewModel.resetState()
            onNavigateToHome()
        }
    }

    // RG8.3 : refus enregistré une seule fois dès l'affichage (survit à un kill avant « Fermer »)
    var refusEnregistre by remember(info.immatriculation) { mutableStateOf(false) }
    LaunchedEffect(info.isMaxAtteint) {
        if (info.isMaxAtteint && !refusEnregistre) {
            refusEnregistre = true
            viewModel.enregistrerRefus(contratId, siteId, info, MESSAGE_MAX_ATTEINT)
        }
    }

    if (showMotifObligatoire) {
        AlertDialog(
            onDismissRequest = { showMotifObligatoire = false },
            text = { Text("Vous devez sélectionner un motif") },
            confirmButton = {
                TextButton(onClick = { showMotifObligatoire = false }) { Text("OK") }
            }
        )
    }

    (saveState as? PassageSaveState.Error)?.let { erreur ->
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            text = { Text("Erreur lors de l'enregistrement : ${erreur.message}") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetState() }) { Text("OK") }
            }
        )
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // RG1.1 / RG1.3 / RG1.6 (RG5.1)
                BlocCard(icon = Icons.Default.Info, titre = "Immatriculation inconnue") {
                    InconnuInfoRow(label = "Immatriculation", value = info.immatriculation)
                    InconnuInfoRow(
                        label = "Passages",
                        // Accords : pluriel seulement à partir de 2 (0/1 passage enregistré, 1 autorisé)
                        value = "${info.nbPassagesDeja} passage${pluriel(info.nbPassagesDeja)} " +
                                "déjà enregistré${pluriel(info.nbPassagesDeja)} sur " +
                                "${info.nbPassagesAutorises} autorisé${pluriel(info.nbPassagesAutorises)} " +
                                "pour une immatriculation inconnue"
                    )
                }

                // RG2 / RG5.2 : motif (liste au 1er passage, libellé ensuite)
                BlocCard(icon = Icons.Default.QuestionMark, titre = "Motif") {
                    if (info.isPremierPassage) {
                        MotifDropdown(
                            motifs = motifs,
                            selection = motifSelectionne,
                            enabled = !isSaving,
                            onSelect = { motifSelectionne = it }
                        )
                    } else {
                        Text(
                            text = info.motifAutorisationLibelle ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // RG8.1 : même rendu que les seuils atteints (rouge + picto Warning)
                if (info.isMaxAtteint) {
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
                            Text(
                                text = MESSAGE_MAX_ATTEINT,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = VeoliaErrorDark
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (info.isMaxAtteint) {
                    // RG8.2 : pas d'enregistrement du passage
                    Button(
                        onClick = onNavigateToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text(text = "Fermer", color = White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                } else {
                    // RG1.5 : contrôle d'accès seul → « Valider »
                    Button(
                        onClick = {
                            val motifId = motifSelectionne?.id ?: info.motifAutorisationContratId
                            val motifLibelle = motifSelectionne?.libelle ?: info.motifAutorisationLibelle
                            if (motifId == null) {
                                showMotifObligatoire = true
                            } else if (!isSaving) {
                                viewModel.enregistrerPassage(contratId, siteId, info, motifId, motifLibelle)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = White)
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
                            text = if (isSaving) "Enregistrement…" else "Valider",
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
private fun BlocCard(icon: ImageVector, titre: String, content: @Composable ColumnScope.() -> Unit) {
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
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = titre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotifDropdown(
    motifs: List<MotifAutorisationContratEntity>,
    selection: MotifAutorisationContratEntity?,
    enabled: Boolean,
    onSelect: (MotifAutorisationContratEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selection?.libelle ?: "Sélectionner",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = VeoliaSubtle,
                focusedBorderColor = VeoliaPrincipal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (motifs.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Aucun motif paramétré pour ce contrat") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
            motifs.forEach { motif ->
                DropdownMenuItem(
                    text = { Text(motif.libelle) },
                    onClick = {
                        onSelect(motif)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InconnuInfoRow(label: String, value: String?) {
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
