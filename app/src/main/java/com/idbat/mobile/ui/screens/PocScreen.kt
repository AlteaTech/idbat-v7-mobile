package com.idbat.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.ui.components.CardData
import com.idbat.mobile.ui.components.CardScanComponent
import com.idbat.mobile.ui.components.PhotoPickerComponent
import com.idbat.mobile.ui.theme.VeoliaPrincipal

private val tabs = listOf("PHOTO", "CB", "RFID Lecture", "RFID Écriture")

@Composable
fun PocScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showCountAlert by remember { mutableStateOf(false) }
    var cardData by remember { mutableStateOf<CardData?>(null) }

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
            0 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PhotoPickerComponent(
                    photos = photos,
                    onPhotosChange = { photos = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showCountAlert = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                ) {
                    Text("Récupérer les photos (${photos.size})")
                }
            }
            1 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CardScanComponent(onCardDataExtracted = { cardData = it })

                cardData?.let { data ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        color = Color.White
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CardField(label = "Numéro", value = data.cardNumber)
                            CardField(label = "Expiration", value = data.expiryDate)
                            CardField(label = "Titulaire", value = data.cardholderName)
                        }
                    }
                }
            }
            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabs[selectedTab],
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }

    if (showCountAlert) {
        AlertDialog(
            onDismissRequest = { showCountAlert = false },
            title = { Text("Photos sélectionnées") },
            text = { Text("${photos.size} photo${if (photos.size > 1) "s" else ""} sélectionnée${if (photos.size > 1) "s" else ""}") },
            confirmButton = {
                TextButton(onClick = { showCountAlert = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun CardField(label: String, value: String?) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value ?: "—",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
