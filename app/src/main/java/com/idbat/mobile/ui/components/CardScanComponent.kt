package com.idbat.mobile.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CardData(
    val cardNumber: String? = null,
    val expiryDate: String? = null,
    val cardholderName: String? = null,
    val cvv: String? = null
)

@Composable
fun CardScanComponent(
    onCardDataExtracted: (CardData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scannedUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showCameraGuide by remember { mutableStateOf(false) }

    fun processUri(uri: Uri) {
        scannedUri = uri
        isProcessing = true
        coroutineScope.launch {
            val data = extractCardData(context, uri)
            isProcessing = false
            onCardDataExtracted(data)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { processUri(it) } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCameraGuide = true
    }

    fun launchCamera() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> showCameraGuide = true
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
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
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "CB", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            scannedUri?.let { uri ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFF27059), Color(0xFFFF9A82))
                        )
                    )
                    .clickable(enabled = !isProcessing) { showSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (scannedUri == null) "Scanner une CB" else "Rescanner",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showCameraGuide) {
        Dialog(
            onDismissRequest = { showCameraGuide = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            CardCameraScreen(
                onPhotoCaptured = { uri ->
                    showCameraGuide = false
                    processUri(uri)
                },
                onDismiss = { showCameraGuide = false }
            )
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Scanner une CB") },
            confirmButton = {
                TextButton(onClick = { showSourceDialog = false; launchCamera() }) {
                    Text("Appareil photo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false; galleryLauncher.launch("image/*") }) {
                    Text("Galerie")
                }
            }
        )
    }
}

private suspend fun extractCardData(context: Context, uri: Uri): CardData =
    suspendCancellableCoroutine { cont ->
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            cont.resume(CardData())
            return@suspendCancellableCoroutine
        }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(parseCardText(result.text))
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(CardData())
            }
    }

private fun parseCardText(text: String): CardData {
    // Format inline : "4165 9816 3566 4976" ou "5136064052794999"
    val inlineRegex = Regex("""\b\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{4}\b""")
    var cardNumber = inlineRegex.find(text)?.value
        ?.replace(Regex("""[\s\-]"""), "")
        ?.chunked(4)?.joinToString(" ")

    // Format vertical : 4 lignes consécutives contenant chacune exactement 4 chiffres
    if (cardNumber == null) {
        val lines = text.lines().map { it.trim() }
        val fourDigitIndices = lines.mapIndexedNotNull { i, line ->
            if (line.matches(Regex("""\d{4}"""))) i else null
        }
        for (i in 0..fourDigitIndices.size - 4) {
            val window = fourDigitIndices.subList(i, i + 4)
            // Les 4 lignes doivent être proches (tolérance de 2 lignes entre chaque)
            if (window.zipWithNext().all { (a, b) -> b - a <= 3 }) {
                cardNumber = window.joinToString(" ") { lines[it] }
                break
            }
        }
    }

    // Date - accepte mois avec ou sans zéro (ex: "06/29" ou "6/29"), et "02/28"
    val expiryRegex = Regex("""\b(0?[1-9]|1[0-2])[/\-]\d{2,4}\b""")
    val expiry = expiryRegex.find(text)?.value

    // CVV - cherche le code après les labels connus (CVV2, CVC, CODE DE SECURITE, CREDIT)
    // ou le code avant le label (format Revolut : "418 CVV2")
    val cvvAfterLabel = Regex(
        """(?:CVV2?|CVC2?|CSC|CODE\s+DE\s+SECURITE|CREDIT)\s*[:\-]?\s*(\d{3,4})""",
        RegexOption.IGNORE_CASE
    ).find(text)?.groupValues?.get(1)

    val cvvBeforeLabel = Regex(
        """(\d{3,4})\s+(?:CVV2?|CVC2?|CSC)""",
        RegexOption.IGNORE_CASE
    ).find(text)?.groupValues?.get(1)

    val cvv = cvvAfterLabel ?: cvvBeforeLabel

    // Nom - ligne tout en majuscules avec au moins 2 mots
    val nameRegex = Regex("""^[A-Z]{2,}(?:\s[A-Z]{2,})+$""", RegexOption.MULTILINE)
    val name = nameRegex.findAll(text)
        .map { it.value.trim() }
        .firstOrNull { it.split(" ").size >= 2 }

    return CardData(cardNumber = cardNumber, expiryDate = expiry, cardholderName = name, cvv = cvv)
}
