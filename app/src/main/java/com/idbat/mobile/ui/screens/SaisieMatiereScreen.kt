package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.MatiereSiteEntity
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.SaisieMatiereLigne
import com.idbat.mobile.ui.theme.*
import com.idbat.mobile.ui.viewmodel.SaisieMatiereViewModel

@Composable
fun SaisieMatiereScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    contrat: ContratEntity?,
    info: InfoCartePassage,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: SaisieMatiereViewModel = hiltViewModel()
) {
    LaunchedEffect(siteId) { viewModel.setSiteId(siteId) }
    LaunchedEffect(Unit) { viewModel.resetLignes() }

    val matieres by viewModel.matieres.collectAsStateWithLifecycle()
    val lignes by viewModel.lignes.collectAsStateWithLifecycle()

    var showValidation by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    // RG4 : index de la ligne en attente de confirmation de suppression
    var deletingIndex by remember { mutableStateOf<Int?>(null) }

    if (showValidation) {
        ConfirmationPassageScreen(
            siteName = siteName,
            siteId = siteId,
            contratId = contratId,
            contrat = contrat,
            info = info,
            lignes = lignes,
            onBack = { showValidation = false },
            onNavigateToHome = onNavigateToHome
        )
        return
    }

    val bgColor = MaterialTheme.colorScheme.background

    // RG4 : dialog de confirmation de suppression
    if (deletingIndex != null) {
        val ligne = lignes.getOrNull(deletingIndex!!)
        AlertDialog(
            onDismissRequest = { deletingIndex = null },
            title = { Text("Supprimer la matière") },
            text = {
                Text("Voulez-vous supprimer « ${ligne?.matiere?.libelle} » ?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.supprimerLigne(deletingIndex!!)
                        deletingIndex = null
                    }
                ) {
                    Text("Supprimer", color = VeoliaErrorDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingIndex = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showDialog) {
        AjouterMatiereDialog(
            matieres = matieres,
            existingLignes = lignes,
            initialLigne = editingIndex?.let { lignes.getOrNull(it) },
            isEditing = editingIndex != null,
            onValidate = { ligne ->
                val idx = editingIndex
                if (idx != null) viewModel.modifierLigne(idx, ligne)
                else viewModel.ajouterLigne(ligne)
                editingIndex = null
                showDialog = false
            },
            onDismiss = {
                editingIndex = null
                showDialog = false
            },
            usagerNom = info.nomTitulaire,
            numeroCarte = info.numeroCarte,
            siteNom = siteName
        )
    }

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

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Matière(s) ajoutée(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = VeoliaSubtle)

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(lignes) { index, ligne ->
                            MatiereLigneRow(
                                ligne = ligne,
                                onEdit = {
                                    editingIndex = index
                                    showDialog = true
                                },
                                // RG4 : on demande une confirmation, pas de suppression directe
                                onDelete = { deletingIndex = index }
                            )
                            if (index < lignes.lastIndex) {
                                HorizontalDivider(color = VeoliaSubtle)
                            }
                        }
                    }

                    HorizontalDivider(color = VeoliaSubtle)
                    Spacer(modifier = Modifier.height(12.dp))

                    // RG3 : désactivé quand toutes les matières disponibles sont déjà ajoutées
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal),
                        enabled = lignes.size < matieres.size
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ajouter une matière",
                            color = White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // RG1 : Continuer désactivé si aucune matière
            Button(
                onClick = { showValidation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                enabled = lignes.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowCircleRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continuer",
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun MatiereLigneRow(
    ligne: SaisieMatiereLigne,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Supprimer",
            tint = VeoliaPrincipal,
            modifier = Modifier
                .size(20.dp)
                .clickable { onDelete() }
        )
        Text(
            text = ligne.matiere.libelle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            // affichage : le point décimal est remplacé par une virgule
            text = "${ligne.quantite.replace('.', ',')} ${ligne.matiere.unitesDesApportLibelle}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifier",
            tint = VeoliaPrincipal,
            modifier = Modifier
                .size(18.dp)
                .clickable { onEdit() }
        )
    }
}

/**
 * Filtre la saisie de quantité : ne conserve que les chiffres et un unique séparateur
 * décimal (point ou virgule). Le séparateur ne peut pas être en première position.
 * Un séparateur en fin est toléré pendant la frappe (ex. "12,") puis nettoyé à la validation.
 */
private fun sanitizeQuantiteInput(input: String): String {
    val sb = StringBuilder()
    var separatorUsed = false
    for (c in input) {
        when {
            c.isDigit() -> sb.append(c)
            c == '.' || c == ',' -> {
                if (sb.isEmpty() || separatorUsed) continue
                separatorUsed = true
                sb.append(c)
            }
        }
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AjouterMatiereDialog(
    matieres: List<MatiereSiteEntity>,
    existingLignes: List<SaisieMatiereLigne>,
    initialLigne: SaisieMatiereLigne?,
    isEditing: Boolean,
    onValidate: (SaisieMatiereLigne) -> Unit,
    onDismiss: () -> Unit,
    usagerNom: String?,
    numeroCarte: String?,
    siteNom: String
) {
    // RG3 : exclure les matières déjà ajoutées, sauf celle en cours d'édition
    val availableMatieres = remember(matieres, existingLignes, initialLigne) {
        matieres.filter { matiere ->
            existingLignes.none { it.matiere.matiereId == matiere.matiereId }
                || initialLigne?.matiere?.matiereId == matiere.matiereId
        }
    }

    var selectedMatiere by remember(initialLigne) {
        mutableStateOf(initialLigne?.matiere ?: availableMatieres.firstOrNull())
    }
    var quantite by remember(initialLigne) {
        // pré-remplissage en édition : on ré-affiche le séparateur en virgule
        mutableStateOf(initialLigne?.quantite?.replace('.', ',') ?: "")
    }
    // RG2 : erreur si quantité vide au moment de valider
    var quantiteError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(availableMatieres) {
        if (selectedMatiere == null) selectedMatiere = availableMatieres.firstOrNull()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Modifier une matière" else "Ajouter une matière",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.5.dp, VeoliaPrincipal, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = VeoliaPrincipal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Matière",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMatiere?.libelle ?: "Sélectionner",
                        onValueChange = {},
                        readOnly = true,
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
                        availableMatieres.forEach { matiere ->
                            DropdownMenuItem(
                                text = { Text(matiere.libelle) },
                                onClick = {
                                    selectedMatiere = matiere
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quantité",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (quantiteError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = quantite,
                        onValueChange = {
                            // RG : chiffres + un seul séparateur (point ou virgule), pas en début
                            quantite = sanitizeQuantiteInput(it)
                            if (quantite.isNotBlank()) quantiteError = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = quantiteError,
                        supportingText = if (quantiteError) {
                            { Text("Obligatoire") }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = VeoliaSubtle,
                            focusedBorderColor = VeoliaPrincipal,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    Text(
                        text = selectedMatiere?.unitesDesApportLibelle ?: "",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val matiere = selectedMatiere ?: return@Button
                        // format numérique stocké : séparateur final supprimé, virgule → point
                        val quantiteNormalisee = quantite.trimEnd('.', ',').replace(',', '.')
                        // RG2 : quantité obligatoire (0 est autorisé, blanc ne l'est pas)
                        if (quantiteNormalisee.isBlank()) {
                            quantiteError = true
                            return@Button
                        }
                        onValidate(
                            SaisieMatiereLigne(
                                matiere = matiere,
                                quantite = quantiteNormalisee,
                                usagerNom = usagerNom,
                                numeroCarte = numeroCarte,
                                date = System.currentTimeMillis(),
                                siteNom = siteNom
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Valider",
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
