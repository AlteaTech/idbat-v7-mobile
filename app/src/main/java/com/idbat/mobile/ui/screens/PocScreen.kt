package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.ui.components.BarcodeScannerComponent
import com.idbat.mobile.ui.components.PhotoPickerComponent
import com.idbat.mobile.ui.theme.VeoliaCoral
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.viewmodel.PocViewModel

private val tabs = listOf("PHOTO", "CB", "RFID Lecture", "RFID Écriture", "Signature")

@Composable
fun PocScreen(
    onBack: () -> Unit,
    viewModel: PocViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = VeoliaCoral
                )
            }
            Text(
                text = "POC",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VeoliaPrincipal
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (uiState.selectedTab) {

            // ── PHOTO ────────────────────────────────────────────────────────
            0 -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PhotoPickerComponent(
                    photos = uiState.photos,
                    onPhotosChange = { viewModel.setPhotos(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.setShowCountAlert(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                ) {
                    Text("Récupérer les photos (${uiState.photos.size})")
                }
            }

            // ── CODE BARRE ───────────────────────────────────────────────────
            1 -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BarcodeScannerComponent(
                    scannedValue = uiState.barcodeValue,
                    onBarcodeDetected = { value, format ->
                        viewModel.setBarcodeResult(value, format)
                    }
                )
                OutlinedTextField(
                    value = uiState.barcodeValue ?: "",
                    onValueChange = { viewModel.setBarcodeResult(it, uiState.barcodeFormat) },
                    label = { Text(uiState.barcodeFormat ?: "Code Barre") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (uiState.barcodeValue != null) {
                        {
                            IconButton(onClick = { viewModel.setBarcodeResult(null, null) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Effacer")
                            }
                        }
                    } else null
                )
            }

            // ── RFID LECTURE ─────────────────────────────────────────────────
          /*  2 -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                MifareReaderComponent(
                    nfcRepository = viewModel.nfcRepository,
                    onCardRead = { viewModel.onCardRead(it) }
                )
            }
            */
            /*
            // ── RFID ÉCRITURE ─────────────────────────────────────────────────
            3 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.rfidCard != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = VeoliaPrincipal.copy(alpha = 0.08f)
                    ) {
                        Text(
                            "Formulaire pré-rempli depuis la lecture (UID : ${uiState.rfidCard!!.uid})",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = VeoliaPrincipal
                        )
                    }
                }

                WriteFormSection("Identité") {
                    OutlinedTextField(
                        value = uiState.fNumeroId,
                        onValueChange = { viewModel.setFNumeroId(it) },
                        label = { Text("N° identification (20 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.fNomPrenom,
                        onValueChange = { viewModel.setFNomPrenom(it) },
                        label = { Text("Nom / Prénom (30 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.fIdentClient,
                        onValueChange = { viewModel.setFIdentClient(it) },
                        label = { Text("Identifiant client (18 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.fSociete,
                        onValueChange = { viewModel.setFSociete(it) },
                        label = { Text("Société (34 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.fMotDePasse,
                        onValueChange = { viewModel.setFMotDePasse(it) },
                        label = { Text("Mot de passe (4 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }

                WriteFormSection("Soldes") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.fSolde,
                            onValueChange = { viewModel.setFSolde(it) },
                            label = { Text("Solde (pts)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = uiState.fCumul,
                            onValueChange = { viewModel.setFCumul(it) },
                            label = { Text("Cumul (pts)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                WriteFormSection("Options") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WriteSwitch("Interne",     uiState.fInterne)     { viewModel.setFInterne(it) }
                        WriteSwitch("Prépaiement", uiState.fPrepaiement) { viewModel.setFPrepaiement(it) }
                        WriteSwitch("Facturation", uiState.fFacturation) { viewModel.setFFacturation(it) }
                        WriteSwitch("Gratuit",     uiState.fGratuit)     { viewModel.setFGratuit(it) }
                    }
                }

                OutlinedTextField(
                    value = uiState.fPaiement,
                    onValueChange = { viewModel.setFPaiement(it) },
                    label = { Text("Paiement comptant (70 car.)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                MifareWriterComponent(
                    nfcRepository = viewModel.nfcRepository,
                    buildCarte = { uid, numSerie ->
                        CartePuce(
                            uid = uid,
                            numeroSerie = numSerie,
                            numeroIdentification = uiState.fNumeroId.take(20),
                            motDePasse = uiState.fMotDePasse.take(4),
                            societe = uiState.fSociete.take(34),
                            interne = uiState.fInterne,
                            prepaiement = uiState.fPrepaiement,
                            facturation = uiState.fFacturation,
                            gratuit = uiState.fGratuit,
                            nomPrenom = uiState.fNomPrenom.take(30),
                            identClient = uiState.fIdentClient.take(18),
                            soldePoints = uiState.fSolde.toDoubleOrNull() ?: 0.0,
                            cumulPoints = uiState.fCumul.toDoubleOrNull() ?: 0.0,
                            paiementComptant = uiState.fPaiement.take(70),
                            crc = ""
                        )
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ── SIGNATURE ────────────────────────────────────────────────────
            4 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val img = uiState.signatureImage
                if (img != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = img,
                            contentDescription = "Signature validée",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    TextButton(
                        onClick = { viewModel.setSignatureImage(null) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Signer à nouveau", color = VeoliaPrincipal)
                    }
                } else {
                    SignatureComponent(onValidate = { viewModel.setSignatureImage(it) })
                }
            }*/
        }
    }

    if (uiState.showCountAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowCountAlert(false) },
            title = { Text("Photos sélectionnées") },
            text = {
                val n = uiState.photos.size
                Text("$n photo${if (n > 1) "s" else ""} sélectionnée${if (n > 1) "s" else ""}")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowCountAlert(false) }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun WriteFormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun WriteSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VeoliaPrincipal,
                checkedTrackColor = VeoliaPrincipal.copy(alpha = 0.4f)
            )
        )
    }
}
