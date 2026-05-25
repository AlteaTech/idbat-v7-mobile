package com.idbat.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idbat.mobile.data.model.CartePuce
import com.idbat.mobile.ui.components.BarcodeScannerComponent
import com.idbat.mobile.ui.components.MifareReaderComponent
import com.idbat.mobile.ui.components.MifareWriterComponent
import com.idbat.mobile.ui.components.PhotoPickerComponent
import com.idbat.mobile.ui.components.SignatureComponent
import com.idbat.mobile.ui.theme.*
import com.idbat.mobile.ui.viewmodel.PocViewModel

private val tabs = listOf("PHOTO", "CB", "RFID Lecture", "RFID Écriture", "Signature")

@Composable
fun PocScreen(
    onBack: () -> Unit,
    viewModel: PocViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Photo ───────────────────────────────────────────────────────────────
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showCountAlert by remember { mutableStateOf(false) }

    // ── Code barre ──────────────────────────────────────────────────────────
    var barcodeValue by remember { mutableStateOf<String?>(null) }
    var barcodeFormat by remember { mutableStateOf<String?>(null) }

    // ── Signature ───────────────────────────────────────────────────────────
    var signatureImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // ── RFID — état partagé lecture ↔ écriture ───────────────────────────────
    var rfidCard by remember { mutableStateOf<CartePuce?>(null) }

    // Champs du formulaire d'écriture (pré-remplis quand rfidCard change)
    var fNumeroId      by remember { mutableStateOf("") }
    var fMotDePasse    by remember { mutableStateOf("") }
    var fSociete       by remember { mutableStateOf("") }
    var fNomPrenom     by remember { mutableStateOf("") }
    var fIdentClient   by remember { mutableStateOf("") }
    var fSolde         by remember { mutableStateOf("0") }
    var fCumul         by remember { mutableStateOf("0") }
    var fPaiement      by remember { mutableStateOf("") }
    var fInterne       by remember { mutableStateOf(false) }
    var fPrepaiement   by remember { mutableStateOf(false) }
    var fFacturation   by remember { mutableStateOf(false) }
    var fGratuit       by remember { mutableStateOf(false) }

    LaunchedEffect(rfidCard) {
        rfidCard?.let { c ->
            fNumeroId    = c.numeroIdentification
            fMotDePasse  = c.motDePasse
            fSociete     = c.societe
            fNomPrenom   = c.nomPrenom
            fIdentClient = c.identClient
            fSolde       = c.soldePoints.toBigDecimal().toPlainString()
            fCumul       = c.cumulPoints.toBigDecimal().toPlainString()
            fPaiement    = c.paiementComptant
            fInterne     = c.interne
            fPrepaiement = c.prepaiement
            fFacturation = c.facturation
            fGratuit     = c.gratuit
        }
    }

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
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VeoliaPrincipal
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {

            // ── PHOTO ───────────────────────────────────────────────────────
            0 -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PhotoPickerComponent(photos = photos, onPhotosChange = { photos = it })
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showCountAlert = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                ) {
                    Text("Récupérer les photos (${photos.size})")
                }
            }

            // ── CODE BARRE ──────────────────────────────────────────────────
            1 -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BarcodeScannerComponent(
                    scannedValue = barcodeValue,
                    onBarcodeDetected = { value, format ->
                        barcodeValue = value
                        barcodeFormat = format
                    }
                )
                OutlinedTextField(
                    value = barcodeValue ?: "",
                    onValueChange = { barcodeValue = it },
                    label = { Text(barcodeFormat ?: "Code Barre") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (barcodeValue != null) {
                        {
                            IconButton(onClick = { barcodeValue = null; barcodeFormat = null }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Effacer")
                            }
                        }
                    } else null
                )
            }

            // ── RFID LECTURE ─────────────────────────────────────────────────
            2 -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                MifareReaderComponent(
                    nfcRepository = viewModel.nfcRepository,
                    onCardRead = { rfidCard = it }
                )
            }

            // ── SIGNATURE ───────────────────────────────────────────────────
            4 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val img = signatureImage
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
                        onClick = { signatureImage = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Signer à nouveau", color = VeoliaPrincipal)
                    }
                } else {
                    SignatureComponent(onValidate = { signatureImage = it })
                }
            }

            // ── RFID ÉCRITURE ─────────────────────────────────────────────────
            3 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (rfidCard != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = VeoliaPrincipal.copy(alpha = 0.08f)
                    ) {
                        Text(
                            "Formulaire pré-rempli depuis la lecture (UID : ${rfidCard!!.uid})",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = VeoliaPrincipal
                        )
                    }
                }

                // Identité
                WriteFormSection("Identité") {
                    OutlinedTextField(
                        value = fNumeroId,
                        onValueChange = { if (it.length <= 20) fNumeroId = it },
                        label = { Text("N° identification (20 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fNomPrenom,
                        onValueChange = { if (it.length <= 30) fNomPrenom = it },
                        label = { Text("Nom / Prénom (30 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fIdentClient,
                        onValueChange = { if (it.length <= 18) fIdentClient = it },
                        label = { Text("Identifiant client (18 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fSociete,
                        onValueChange = { if (it.length <= 34) fSociete = it },
                        label = { Text("Société (34 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fMotDePasse,
                        onValueChange = { if (it.length <= 4) fMotDePasse = it },
                        label = { Text("Mot de passe (4 car.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }

                // Soldes
                WriteFormSection("Soldes") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = fSolde,
                            onValueChange = { fSolde = it },
                            label = { Text("Solde (pts)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = fCumul,
                            onValueChange = { fCumul = it },
                            label = { Text("Cumul (pts)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                // Options
                WriteFormSection("Options") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WriteSwitch("Interne",      fInterne)      { fInterne = it }
                        WriteSwitch("Prépaiement",  fPrepaiement)  { fPrepaiement = it }
                        WriteSwitch("Facturation",  fFacturation)  { fFacturation = it }
                        WriteSwitch("Gratuit",      fGratuit)      { fGratuit = it }
                    }
                }

                // Paiement comptant (champ long, optionnel)
                OutlinedTextField(
                    value = fPaiement,
                    onValueChange = { if (it.length <= 70) fPaiement = it },
                    label = { Text("Paiement comptant (70 car.)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Composant NFC — buildCarte appelé avec l'UID réel de la carte cible
                MifareWriterComponent(
                    nfcRepository = viewModel.nfcRepository,
                    buildCarte = { uid, numSerie ->
                        CartePuce(
                            uid = uid,
                            numeroSerie = numSerie,
                            numeroIdentification = fNumeroId.take(20),
                            motDePasse = fMotDePasse.take(4),
                            societe = fSociete.take(34),
                            interne = fInterne,
                            prepaiement = fPrepaiement,
                            facturation = fFacturation,
                            gratuit = fGratuit,
                            nomPrenom = fNomPrenom.take(30),
                            identClient = fIdentClient.take(18),
                            soldePoints = fSolde.toDoubleOrNull() ?: 0.0,
                            cumulPoints = fCumul.toDoubleOrNull() ?: 0.0,
                            paiementComptant = fPaiement.take(70),
                            crc = "" // recalculé par serialize()
                        )
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showCountAlert) {
        AlertDialog(
            onDismissRequest = { showCountAlert = false },
            title = { Text("Photos sélectionnées") },
            text = {
                Text(
                    "${photos.size} photo${if (photos.size > 1) "s" else ""} " +
                        "sélectionnée${if (photos.size > 1) "s" else ""}"
                )
            },
            confirmButton = {
                TextButton(onClick = { showCountAlert = false }) { Text("OK") }
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
            color = Color.Gray
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
            colors = SwitchDefaults.colors(checkedThumbColor = VeoliaPrincipal, checkedTrackColor = VeoliaPrincipal.copy(alpha = 0.4f))
        )
    }
}
