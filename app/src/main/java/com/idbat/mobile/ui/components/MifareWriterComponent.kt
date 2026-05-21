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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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

private sealed class WriteState {
    object Idle : WriteState()
    object Writing : WriteState()
    data class Success(val uid: String, val techType: String, val bytesWritten: Int) : WriteState()
    data class Error(val message: String) : WriteState()
}

@Composable
fun MifareWriterComponent(textToWrite: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var writeState by remember { mutableStateOf<WriteState>(WriteState.Idle) }

    fun handleTag(tag: Tag) {
        if (textToWrite.isBlank()) return
        writeState = WriteState.Writing
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val result = writeMifareTag(tag, textToWrite)
                withContext(Dispatchers.Main) { writeState = result }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    writeState = WriteState.Error(e.message ?: "Erreur d'écriture")
                }
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
                Icon(Icons.Outlined.Nfc, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RFID Écriture", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = writeState) {
                WriteState.Idle -> WaitingToWrite(ready = textToWrite.isNotBlank())
                WriteState.Writing -> WritingIndicator()
                is WriteState.Success -> WriteSuccessDisplay(state) { writeState = WriteState.Idle }
                is WriteState.Error -> WriteErrorDisplay(state.message) { writeState = WriteState.Idle }
            }
        }
    }
}

@Composable
private fun WaitingToWrite(ready: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_write_pulse")
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
                .alpha(if (ready) alpha else 0.2f),
            tint = if (ready) VeoliaPrincipal else Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (ready) "Approchez une carte pour écrire" else "Saisissez un texte ci-dessus",
            fontSize = 15.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun WritingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = VeoliaPrincipal)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Écriture en cours…", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun WriteSuccessDisplay(state: WriteState.Success, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Écriture réussie",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp
                    )
                    Text("${state.bytesWritten} octets écrits", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = VeoliaPrincipal.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                Text("UID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VeoliaPrincipal)
                Text(
                    text = state.uid,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(state.techType, fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
            Text("Nouvelle écriture", color = VeoliaPrincipal)
        }
    }
}

@Composable
private fun WriteErrorDisplay(message: String, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Erreur d'écriture", fontWeight = FontWeight.Bold, color = Color.Red)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onReset) { Text("Réessayer", color = VeoliaPrincipal) }
    }
}

private fun writeMifareTag(tag: Tag, text: String): WriteState.Success {
    val uid = tag.id.toWriterHex()
    val techs = tag.techList.toList()
    val bytes = text.toByteArray(Charsets.UTF_8)
    return when {
        MifareClassic::class.java.name in techs -> writeMifareClassic(tag, uid, bytes)
        MifareUltralight::class.java.name in techs -> writeMifareUltralight(tag, uid, bytes)
        else -> throw Exception("Format de carte non supporté")
    }
}

private fun writeMifareClassic(tag: Tag, uid: String, bytes: ByteArray): WriteState.Success {
    val padded = bytes.copyOf(((bytes.size + 15) / 16) * 16)
    val mifare = MifareClassic.get(tag) ?: throw Exception("Carte non supportée")
    var byteOffset = 0
    var written = 0

    mifare.use { card ->
        card.connect()

        // Sector 0 : skip block 0 (fabricant, lecture seule), écrire blocks 1 et 2
        val auth0 = card.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT)
            || card.authenticateSectorWithKeyA(0, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
            || card.authenticateSectorWithKeyB(0, MifareClassic.KEY_DEFAULT)
        if (auth0) {
            for (offset in 1..2) {
                if (byteOffset >= padded.size) break
                card.writeBlock(card.sectorToBlock(0) + offset, padded.sliceArray(byteOffset until byteOffset + 16))
                byteOffset += 16; written += 16
            }
        }

        // Sectors 1+
        for (s in 1 until card.sectorCount) {
            if (byteOffset >= padded.size) break
            val auth = card.authenticateSectorWithKeyA(s, MifareClassic.KEY_DEFAULT)
                || card.authenticateSectorWithKeyA(s, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                || card.authenticateSectorWithKeyB(s, MifareClassic.KEY_DEFAULT)
            if (auth) {
                val firstBlock = card.sectorToBlock(s)
                for (b in 0 until card.getBlockCountInSector(s) - 1) { // -1 : skip trailer
                    if (byteOffset >= padded.size) break
                    card.writeBlock(firstBlock + b, padded.sliceArray(byteOffset until byteOffset + 16))
                    byteOffset += 16; written += 16
                }
            }
        }

        if (byteOffset < padded.size) throw Exception("Espace insuffisant sur la carte")
    }
    return WriteState.Success(uid, "Mifare Classic", written)
}

private fun writeMifareUltralight(tag: Tag, uid: String, bytes: ByteArray): WriteState.Success {
    val padded = bytes.copyOf(((bytes.size + 3) / 4) * 4)
    val pagesNeeded = padded.size / 4
    if (pagesNeeded > 12) throw Exception("Texte trop long (${12 * 4} octets max pour Ultralight)")
    val mifare = MifareUltralight.get(tag) ?: throw Exception("Carte non supportée")
    var written = 0

    mifare.use { card ->
        card.connect()
        for (i in 0 until pagesNeeded) {
            card.writePage(4 + i, padded.sliceArray(i * 4 until (i + 1) * 4))
            written += 4
        }
    }
    return WriteState.Success(uid, "Mifare Ultralight", written)
}

private fun ByteArray.toWriterHex(): String = joinToString(" ") { "%02X".format(it) }
