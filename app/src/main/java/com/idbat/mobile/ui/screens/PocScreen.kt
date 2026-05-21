package com.idbat.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.ui.components.BarcodeScannerComponent
import com.idbat.mobile.ui.components.CartePuce
import com.idbat.mobile.ui.components.MifareReaderComponent
import com.idbat.mobile.ui.components.MifareWriterComponent
import com.idbat.mobile.ui.components.PhotoPickerComponent
import com.idbat.mobile.ui.theme.VeoliaPrincipal

private val tabs = listOf("PHOTO", "CB", "RFID Lecture", "RFID Écriture")

@Composable
fun PocScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── État photo ──────────────────────────────────────────────────────────
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showCountAlert by remember { mutableStateOf(false) }

    // ── État code barre ─────────────────────────────────────────────────────
    var barcodeValue by remember { mutableStateOf<String?>(null) }
    var barcodeFormat by remember { mutableStateOf<String?>(null) }

    // ── État RFID (partagé lecture ↔ écriture) ──────────────────────────────
    var rfidCard by remember { mutableStateOf<CartePuce?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                    tint = Color(0xFFF27059)
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
            containerColor = Color.White,
            contentColor = VeoliaPrincipal
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
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
                MifareReaderComponent(onCardRead = { rfidCard = it })
            }

            // ── RFID ÉCRITURE ────────────────────────────────────────────────
            3 -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
           //     MifareWriterComponent(cartePuce = rfidCard)
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
