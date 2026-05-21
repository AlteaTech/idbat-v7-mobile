package com.idbat.mobile.ui.components

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.os.Build
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
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Consumer
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MifareCardData(
    val uid: String,
    val techType: String,
    val sectors: List<Pair<String, String>> = emptyList()
)

@Composable
fun MifareReaderComponent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var cardData by remember { mutableStateOf<MifareCardData?>(null) }
    var isReading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleTag(tag: Tag) {
        isReading = true
        errorMessage = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val data = readMifareTag(tag)
                withContext(Dispatchers.Main) { cardData = data; isReading = false }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMessage = e.message ?: "Erreur de lecture"; isReading = false }
            }
        }
    }

    DisposableEffect(Unit) {
        if (nfcAdapter == null || activity == null) return@DisposableEffect onDispose {}

        val intent = Intent(context, activity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val techLists = arrayOf(
            arrayOf(MifareClassic::class.java.name),
            arrayOf(MifareUltralight::class.java.name)
        )
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, techLists)

        val listener = Consumer<Intent> { receivedIntent ->
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                receivedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            else
                @Suppress("DEPRECATION")
                receivedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { handleTag(it) }
        }
        activity.addOnNewIntentListener(listener)

        onDispose {
            activity.removeOnNewIntentListener(listener)
            try { nfcAdapter.disableForegroundDispatch(activity) } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("RFID Mifare", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                nfcAdapter == null -> NfcUnavailableMessage()
                isReading -> ReadingIndicator()
                cardData != null -> CardDataDisplay(cardData!!, onReset = { cardData = null })
                errorMessage != null -> ErrorDisplay(errorMessage!!, onReset = { errorMessage = null })
                else -> WaitingForCard()
            }
        }
    }
}

@Composable
private fun WaitingForCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Nfc,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha(alpha),
            tint = VeoliaPrincipal
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Approchez une carte Mifare",
            fontSize = 15.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun ReadingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = VeoliaPrincipal)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Lecture en cours…", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun CardDataDisplay(data: MifareCardData, onReset: () -> Unit) {
    Column {
        // UID
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = VeoliaPrincipal.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                Text("UID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VeoliaPrincipal)
                Text(
                    text = data.uid,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(data.techType, fontSize = 11.sp, color = Color.Gray)
            }
        }

        if (data.sectors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Données lues",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.sectors.forEach { (label, hex) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
                        Text(
                            hex,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
            Text("Nouvelle lecture", color = VeoliaPrincipal)
        }
    }
}

@Composable
private fun ErrorDisplay(message: String, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Erreur de lecture", fontWeight = FontWeight.Bold, color = Color.Red)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onReset) { Text("Réessayer", color = VeoliaPrincipal) }
    }
}

@Composable
private fun NfcUnavailableMessage() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("NFC non disponible sur cet appareil", color = Color.Gray, fontSize = 14.sp)
    }
}

private fun readMifareTag(tag: Tag): MifareCardData {
    val uid = tag.id.toHex()
    val techs = tag.techList.toList()

    return when {
        MifareClassic::class.java.name in techs -> readMifareClassic(tag, uid)
        MifareUltralight::class.java.name in techs -> readMifareUltralight(tag, uid)
        else -> MifareCardData(
            uid = uid,
            techType = techs.lastOrNull()?.substringAfterLast(".") ?: "Inconnu"
        )
    }
}

private fun readMifareClassic(tag: Tag, uid: String): MifareCardData {
    val mifare = MifareClassic.get(tag) ?: return MifareCardData(uid, "MifareClassic")
    val sectors = mutableListOf<Pair<String, String>>()
    mifare.use { card ->
        card.connect()
        for (s in 0 until card.sectorCount) {
            val auth = card.authenticateSectorWithKeyA(s, MifareClassic.KEY_DEFAULT)
                || card.authenticateSectorWithKeyA(s, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                || card.authenticateSectorWithKeyB(s, MifareClassic.KEY_DEFAULT)
            if (auth) {
                val firstBlock = card.sectorToBlock(s)
                val blockCount = card.getBlockCountInSector(s)
                for (b in 0 until blockCount - 1) { // -1 : on skip le trailer
                    val data = card.readBlock(firstBlock + b)
                    if (data.any { it != 0.toByte() })
                        sectors.add("S$s B$b" to data.toHex())
                }
            }
        }
    }
    return MifareCardData(uid = uid, techType = "Mifare Classic", sectors = sectors)
}

private fun readMifareUltralight(tag: Tag, uid: String): MifareCardData {
    val mifare = MifareUltralight.get(tag) ?: return MifareCardData(uid, "MifareUltralight")
    val pages = mutableListOf<Pair<String, String>>()
    mifare.use { card ->
        card.connect()
        var page = 4
        while (page < 16) {
            try {
                val data = card.readPages(page)
                for (i in 0..3) {
                    val offset = i * 4
                    val block = data.sliceArray(offset until (offset + 4))
                    if (block.any { it != 0.toByte() })
                        pages.add("P${page + i}" to block.toHex())
                }
                page += 4
            } catch (_: Exception) { break }
        }
    }
    return MifareCardData(uid = uid, techType = "Mifare Ultralight", sectors = pages)
}

private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
