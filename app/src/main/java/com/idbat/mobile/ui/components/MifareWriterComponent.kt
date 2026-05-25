package com.idbat.mobile.ui.components

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
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
import com.idbat.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MifareWriter"

private sealed class WriteState {
    object Idle : WriteState()
    object Writing : WriteState()
    data class Success(val uid: String) : WriteState()
    data class Error(val message: String) : WriteState()
}

/**
 * Composant d'écriture Mifare.
 *
 * [buildCarte] est appelé au moment de la détection NFC avec l'UID réel de la carte cible,
 * ce qui garantit que le CRC est calculé avec le bon numéro de série.
 * Retourner null annule l'écriture sans erreur.
 */
@Composable
fun MifareWriterComponent(
    buildCarte: (uid: String, numeroSerie: String) -> CartePuce?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var writeState by remember { mutableStateOf<WriteState>(WriteState.Idle) }
    val currentBuilder = rememberUpdatedState(buildCarte)

    DisposableEffect(Unit) {
        if (nfcAdapter == null || activity == null) return@DisposableEffect onDispose {}

        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                if (tag == null) return@enableReaderMode
                val uid = tag.id.joinToString(" ") { "%02X".format(it) }
                val numSerie = tag.id.take(4).joinToString("") { "%02X".format(it) }
                val carte = currentBuilder.value(uid, numSerie) ?: return@enableReaderMode

                coroutineScope.launch(Dispatchers.Main) { writeState = WriteState.Writing }
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        writeCartePuce(tag, carte)
                        withContext(Dispatchers.Main) { writeState = WriteState.Success(uid) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            writeState = WriteState.Error(e.message ?: "Erreur d'écriture")
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
                Text("Écriture NFC", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (nfcAdapter == null) {
                Text("NFC non disponible sur cet appareil", color = Color.Gray, fontSize = 14.sp)
                return@Column
            }

            when (val state = writeState) {
                WriteState.Idle -> WriteReadyIdle()
                WriteState.Writing -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = VeoliaPrincipal)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Écriture en cours…", fontSize = 14.sp, color = Color.Gray)
                }
                is WriteState.Success -> Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = VeoliaSuccess.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Check, null,
                                tint = VeoliaSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Écriture réussie",
                                    fontWeight = FontWeight.Bold,
                                    color = VeoliaSuccess,
                                    fontSize = 14.sp
                                )
                                Text(
                                    state.uid,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { writeState = WriteState.Idle },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Nouvelle écriture", color = VeoliaPrincipal) }
                }
                is WriteState.Error -> Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Erreur d'écriture", fontWeight = FontWeight.Bold, color = VeoliaErrorDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(state.message, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { writeState = WriteState.Idle }) {
                        Text("Réessayer", color = VeoliaPrincipal)
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteReadyIdle() {
    val transition = rememberInfiniteTransition(label = "nfc_write_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Nfc, null,
            modifier = Modifier.size(56.dp).alpha(alpha),
            tint = VeoliaPrincipal
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Approchez la carte à écrire", fontSize = 14.sp, color = Color.Gray)
    }
}

// ── Écriture Mifare ─────────────────────────────────────────────────────────

private fun writeCartePuce(tag: Tag, carte: CartePuce) {
    val content = carte.serialize()
    check(content.length == 240) { "Sérialisation invalide (${content.length}/240)" }
    val bytes = content.toByteArray(Charsets.ISO_8859_1)

    data class Block(val sector: Int, val offset: Int)

    val sequence = buildList {
        add(Block(0, 1)); add(Block(0, 2))
        for (s in 1..4) { add(Block(s, 0)); add(Block(s, 1)); add(Block(s, 2)) }
        add(Block(5, 0))
    }

    val mifare = MifareClassic.get(tag) ?: throw Exception("Carte non supportée")

    mifare.use { card ->
        card.connect()
        var authenticatedSector = -1

        sequence.forEachIndexed { i, blk ->
            if (blk.sector != authenticatedSector) {
                val ok = card.authenticateSectorWithKeyB(blk.sector, MifareClassic.KEY_DEFAULT)
                    || card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_DEFAULT)
                    || card.authenticateSectorWithKeyA(blk.sector, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
                if (!ok) throw Exception("Authentification échouée — secteur ${blk.sector}")
                authenticatedSector = blk.sector
            }

            val blockIndex = card.sectorToBlock(blk.sector) + blk.offset
            val data = bytes.sliceArray(i * 16 until (i + 1) * 16)

            Log.d(TAG, "writeBlock($blockIndex) [S${blk.sector}B${blk.offset}]")
            card.writeBlock(blockIndex, data)

            val readBack = card.readBlock(blockIndex)
            if (!readBack.contentEquals(data))
                throw Exception("Vérification échouée — S${blk.sector}B${blk.offset}")
        }
    }

    Log.d(TAG, "Écriture OK — UID ${carte.uid}")
}
