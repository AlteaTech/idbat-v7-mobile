package com.idbat.mobile.ui.components

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MifareReaderComponent(
    onCardRead: (CartePuce) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var cardData by remember { mutableStateOf<CartePuce?>(null) }
    var isReading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        if (nfcAdapter == null || activity == null) return@DisposableEffect onDispose {}

        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                if (tag == null) return@enableReaderMode
                coroutineScope.launch(Dispatchers.Main) { isReading = true; errorMessage = null }
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val data = readCartePuce(tag)
                        withContext(Dispatchers.Main) {
                            cardData = data
                            isReading = false
                            onCardRead(data)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorMessage = e.message ?: "Erreur de lecture"
                            isReading = false
                        }
                    }
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )

        onDispose {
            try { nfcAdapter.disableReaderMode(activity) } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Nfc, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RFID Lecture", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                nfcAdapter == null -> Text(
                    "NFC non disponible sur cet appareil",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                isReading -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = VeoliaPrincipal)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Lecture en cours…", fontSize = 14.sp, color = Color.Gray)
                }
                cardData != null -> CartePuceDisplay(
                    carte = cardData!!,
                    onReset = { cardData = null }
                )
                errorMessage != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Erreur de lecture", fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage!!, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { errorMessage = null }) {
                        Text("Réessayer", color = VeoliaPrincipal)
                    }
                }
                else -> WaitingForCardRead()
            }
        }
    }
}

@Composable
private fun WaitingForCardRead() {
    val transition = rememberInfiniteTransition(label = "nfc_read_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Nfc,
            contentDescription = null,
            modifier = Modifier.size(64.dp).alpha(alpha),
            tint = VeoliaPrincipal
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Approchez une carte Mifare", fontSize = 15.sp, color = Color.Gray)
    }
}

@Composable
private fun CartePuceDisplay(carte: CartePuce, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // UID + CRC validity
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = VeoliaPrincipal.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("UID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VeoliaPrincipal)
                    Text(carte.uid, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("N° série : ${carte.numeroSerie}", fontSize = 11.sp, color = Color.Gray)
                }
                Icon(
                    imageVector = if (carte.isCrcValid) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = if (carte.isCrcValid) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Identité
        CartePuceSection("Identification") {
            CartePuceField("N° identification", carte.numeroIdentification)
            CartePuceField("Société", carte.societe)
            CartePuceField("Nom / Prénom", carte.nomPrenom)
            CartePuceField("Client", carte.identClient)
        }

        // Points
        CartePuceSection("Soldes") {
            CartePuceField("Solde points", "%.2f pts".format(carte.soldePoints))
            CartePuceField("Cumul points", "%.2f pts".format(carte.cumulPoints))
        }

        // Flags
        CartePuceSection("Options") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CartePuceChip("Interne", carte.interne)
                CartePuceChip("Prépaiement", carte.prepaiement)
                CartePuceChip("Facturation", carte.facturation)
                CartePuceChip("Gratuit", carte.gratuit)
            }
        }

        // CRC
        CartePuceSection("Sécurité") {
            CartePuceField(
                "CRC",
                if (carte.isCrcValid) "Valide ✓" else "Invalide ✗",
                valueColor = if (carte.isCrcValid) Color(0xFF4CAF50) else Color.Red
            )
        }

        TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
            Text("Nouvelle lecture", color = VeoliaPrincipal)
        }
    }
}

@Composable
private fun CartePuceSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun CartePuceField(label: String, value: String, valueColor: Color = Color.Unspecified) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun CartePuceChip(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) VeoliaPrincipal else Color(0xFFE0E0E0)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            color = if (active) Color.White else Color.Gray,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Lecture Mifare ──────────────────────────────────────────────────────────

/**
 * Lit les 240 octets du CartePuce depuis les blocs Mifare Classic.
 * Layout : S0B1, S0B2, S1B0-B2, S2B0-B2, S3B0-B2, S4B0-B2, S5B0 (15 blocs × 16 = 240 bytes)
 */
private fun readCartePuce(tag: Tag): CartePuce {
    val uid = tag.id.joinToString(" ") { "%02X".format(it) }
    val numeroSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }

    if (MifareClassic::class.java.name !in tag.techList)
        throw Exception("Carte non supportée (Mifare Classic requis)")

    val mifare = MifareClassic.get(tag) ?: throw Exception("Impossible d'accéder à la carte")
    val contentBytes = ByteArray(240)

    data class Block(val sector: Int, val offset: Int)

    val sequence = buildList {
        add(Block(0, 1)); add(Block(0, 2))
        for (s in 1..4) { add(Block(s, 0)); add(Block(s, 1)); add(Block(s, 2)) }
        add(Block(5, 0))
    }

    mifare.use { card ->
        card.connect()
        var authenticatedSector = -1
        sequence.forEachIndexed { i, blk ->
            if (blk.sector != authenticatedSector) {
                val ok = card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_DEFAULT)
                    || card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                    || card.authenticateSectorWithKeyB(blk.sector, MifareClassic.KEY_DEFAULT)
                if (!ok) throw Exception("Authentification échouée — secteur ${blk.sector}")
                authenticatedSector = blk.sector
            }
            card.readBlock(card.sectorToBlock(blk.sector) + blk.offset)
                .copyInto(contentBytes, i * 16)
        }
    }

    return CartePuce.parse(uid, numeroSerie, String(contentBytes, Charsets.ISO_8859_1))
}
