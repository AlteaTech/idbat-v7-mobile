package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.data.entities.UsagerEntity
import com.idbat.mobile.ui.theme.*
import com.idbat.mobile.ui.viewmodel.AutresCartesViewModel
import com.idbat.mobile.ui.viewmodel.ContratViewModel

@Composable
fun AutresCartesScreen(
    siteName: String,
    siteId: Long,
    contratId: Long,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: AutresCartesViewModel = hiltViewModel(),
    contratViewModel: ContratViewModel = hiltViewModel()
) {
    LaunchedEffect(contratId) {
        viewModel.setContratId(contratId)
        contratViewModel.setContratId(contratId)
    }
    val contrat by contratViewModel.contrat.collectAsStateWithLifecycle()

    val bgColor = MaterialTheme.colorScheme.background
    val showCodebarres = contrat?.hasCodebarres == true
    val showImmatriculation = contrat?.hasImmatriculation == true
    val showSelectionUsager = contrat?.hasSelectionusager == true

    val usagers by viewModel.usagers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val passageInfo by viewModel.passageInfo.collectAsStateWithLifecycle()
    val carteInconnue by viewModel.carteInconnue.collectAsStateWithLifecycle()

    var codebarresValue by remember { mutableStateOf("") }
    var immatriculationValue by remember { mutableStateOf("") }
    var selectedUsager by remember { mutableStateOf<UsagerEntity?>(null) }
    var showUsagerDialog by remember { mutableStateOf(false) }
    var showCarteInconnueDialog by remember { mutableStateOf(false) }

    LaunchedEffect(carteInconnue) {
        if (carteInconnue) showCarteInconnueDialog = true
    }

    var showSaisieMatiere by remember { mutableStateOf(false) }

    if (passageInfo != null && showSaisieMatiere) {
        SaisieMatiereScreen(
            siteName = siteName,
            siteId = siteId,
            contratId = contratId,
            info = passageInfo!!,
            onBack = { showSaisieMatiere = false },
            onNavigateToHome = {
                viewModel.clearPassageInfo()
                viewModel.clearCarteInconnue()
                showSaisieMatiere = false
                onNavigateToHome()
            }
        )
        return
    }

    if (passageInfo != null) {
        PassageInfoScreen(
            siteName = siteName,
            siteId = siteId,
            contratId = contratId,
            info = passageInfo!!,
            onBack = { viewModel.clearPassageInfo() },
            onNavigateToHome = {
                viewModel.clearPassageInfo()
                viewModel.clearCarteInconnue()
                onNavigateToHome()
            },
            onSaisirMatieres = { showSaisieMatiere = true }
        )
        return
    }

    if (showUsagerDialog) {
        UsagerSelectionDialog(
            usagers = usagers,
            searchQuery = searchQuery,
            onSearchChange = { viewModel.updateSearch(it) },
            onSelect = { usager ->
                selectedUsager = usager
                codebarresValue = ""
                immatriculationValue = ""
                viewModel.clearSearch()
                showUsagerDialog = false
            },
            onDismiss = {
                viewModel.clearSearch()
                showUsagerDialog = false
            }
        )
    }

    if (showCarteInconnueDialog) {
        AlertDialog(
            onDismissRequest = {
                immatriculationValue = ""
                viewModel.clearCarteInconnue()
                showCarteInconnueDialog = false
            },
            text = { Text("Carte inconnue") },
            confirmButton = {
                TextButton(onClick = {
                    immatriculationValue = ""
                    viewModel.clearCarteInconnue()
                    showCarteInconnueDialog = false
                }) {
                    Text("OK")
                }
            }
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showCodebarres) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Code-barres ou QR code",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Scannez la carte ou tapez le code",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeoliaPrincipal
                                    )
                                ) {
                                    Text(
                                        text = "Scanner",
                                        color = White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                OutlinedTextField(
                                    value = codebarresValue,
                                    onValueChange = {
                                        codebarresValue = it
                                        immatriculationValue = ""
                                        selectedUsager = null
                                    },
                                    placeholder = {
                                        Text(text = "Taper le code", color = VeoliaPlaceholder)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = VeoliaSubtle,
                                        focusedBorderColor = VeoliaPrincipal
                                    )
                                )
                            }
                        }
                    }
                }

                if (showImmatriculation) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "123",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Immatriculation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Immatriculation",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = immatriculationValue,
                                    onValueChange = { input ->
                                        // Uppercase + alphanumérique non accentué uniquement
                                        immatriculationValue = input
                                            .uppercase()
                                            .filter { it in 'A'..'Z' || it in '0'..'9' }
                                        codebarresValue = ""
                                        selectedUsager = null
                                    },
                                    placeholder = {
                                        Text(text = "AA123AA", color = VeoliaPlaceholder)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        keyboardType = KeyboardType.Ascii
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = VeoliaSubtle,
                                        focusedBorderColor = VeoliaPrincipal
                                    )
                                )
                            }
                        }
                    }
                }

                if (showSelectionUsager) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sélection d'un usager",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Nom, Prénom",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                UsagerSelectField(
                                    selectedUsager = selectedUsager,
                                    onClick = { showUsagerDialog = true }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            var showEmptyInputDialog by remember { mutableStateOf(false) }

            if (showEmptyInputDialog) {
                AlertDialog(
                    onDismissRequest = { showEmptyInputDialog = false },
                    text = {
                        Text("Vous devez renseigner une information d'identification de l'usager")
                    },
                    confirmButton = {
                        TextButton(onClick = { showEmptyInputDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            val hasInput = selectedUsager != null
                    || codebarresValue.isNotBlank()
                    || immatriculationValue.isNotBlank()

            Button(
                onClick = {
                    if (hasInput) viewModel.valider(selectedUsager, codebarresValue, immatriculationValue)
                    else showEmptyInputDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
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

@Composable
private fun UsagerSelectField(
    selectedUsager: UsagerEntity?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, VeoliaSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedUsager?.let { "${it.nom.uppercase()} ${it.prenom}" }
                    ?: "Sélectionner",
                color = if (selectedUsager != null)
                    MaterialTheme.colorScheme.onSurface
                else
                    VeoliaPlaceholder,
                fontSize = 16.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = VeoliaPrincipal
            )
        }
    }
}

@Composable
private fun UsagerSelectionDialog(
    usagers: List<UsagerEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (UsagerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sélectionnez l'usager",
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(text = "Rechercher", color = VeoliaPlaceholder)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = VeoliaSubtle,
                            focusedBorderColor = VeoliaPrincipal
                        )
                    )
                    Button(
                        onClick = { },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    items(usagers, key = { it.id }) { usager ->
                        Text(
                            text = "${usager.nom.uppercase()} ${usager.prenom}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(usager) }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        HorizontalDivider(color = VeoliaSubtle)
                    }
                }
            }
        }
    }
}
