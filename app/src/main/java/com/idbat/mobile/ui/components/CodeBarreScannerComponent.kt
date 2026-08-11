package com.idbat.mobile.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.idbat.mobile.data.model.toFormatName
import com.idbat.mobile.ui.theme.VeoliaCoral
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.theme.VeoliaSuccess
import java.util.concurrent.Executors

/**
 * Scanner dédié aux **codes-barres linéaires (1D) uniquement** — QR code, DataMatrix, PDF417 et
 * autres formats 2D sont volontairement exclus (voir [linearBarcodeScanner]). La fenêtre de visée
 * est un rectangle large adapté à la forme d'un code-barres. La caméra ne démarre qu'au clic sur
 * "Scanner" ; à la détection, `onBarcodeDetected(value, format)` est appelé et la caméra se coupe.
 */
@Composable
fun CodeBarreScannerComponent(
    scannedValue: String?,
    onBarcodeDetected: (value: String, format: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Code-barres",
    subtitle: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var isActive by remember { mutableStateOf(true) }
    // La caméra ne démarre qu'au clic sur "Scanner"
    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(scannedValue) {
        if (scannedValue == null) isActive = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) { isActive = true; isScanning = true }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ViewWeek, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            if (!isScanning) {
                // Bouton de lancement : la caméra ne s'ouvre qu'ici
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.horizontalGradient(listOf(VeoliaCoral, VeoliaPrincipal))
                        )
                        .clickable {
                            if (hasPermission) { isActive = true; isScanning = true }
                            else permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Preview caméra avec analyse en temps réel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                ) {
                    val executor = remember { Executors.newSingleThreadExecutor() }
                    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
                    // Anti-lecture-partielle : n'accepte une valeur qu'après N frames identiques
                    val confirmer = remember { StableBarcodeConfirmer(REQUIRED_CONFIRMATIONS, WARMUP_MS) }
                    // Libère la caméra quand on quitte le scan (sinon elle reste liée au lifecycle
                    // de la route et tourne en arrière-plan — peut gêner d'autres usages matériels).
                    DisposableEffect(Unit) {
                        onDispose {
                            try { cameraProvider?.unbindAll() } catch (_: Exception) {}
                            executor.shutdown()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                val provider = future.get()
                                cameraProvider = provider
                                val preview = Preview.Builder().build()
                                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                                val analyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(executor) { proxy ->
                                            if (isActive) analyzeLinearFrame(proxy) { value, format ->
                                                // Confirmé seulement si lu N fois de suite à l'identique
                                                if (confirmer.confirm(value)) {
                                                    isActive = false
                                                    isScanning = false
                                                    onBarcodeDetected(value, format)
                                                }
                                            } else proxy.close()
                                        }
                                    }

                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analyzer
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Calque de guidage : fenêtre large adaptée à un code-barres 1D
                    BarcodeOverlay(found = !isActive)

                    if (isActive) {
                        Text(
                            text = "Alignez le code-barres ou le QR dans le cadre",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeOverlay(found: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        // RG1 : fenêtre carrée centrée — lit le QR IDemat comme les CB simples (dans tous les sens)
        val side = size.width * 0.7f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val corner = CornerRadius(12.dp.toPx())
        val accentColor = if (found) VeoliaSuccess else VeoliaCoral

        drawRect(Color.Black.copy(alpha = 0.45f))
        drawRoundRect(
            Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = corner,
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            accentColor,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = corner,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

/** Nombre de lectures identiques consécutives requises pour valider un code-barres. */
private const val REQUIRED_CONFIRMATIONS = 5

/**
 * Période de chauffe (ms) : on ignore toute lecture pendant ce délai après la 1ʳᵉ frame reçue, le
 * temps que l'autofocus se stabilise (les frames floues du démarrage sont la principale source de
 * fragments parasites).
 */
private const val WARMUP_MS = 800L

/**
 * Filtre anti-lecture-partielle en deux temps :
 * 1. **Chauffe** ([warmupMs]) : toute lecture est ignorée le temps que la caméra fasse le point.
 * 2. **Confirmation** : une valeur n'est validée qu'après avoir été lue [required] fois **de suite
 *    à l'identique**. Les fragments parasites (qui changent d'une frame à l'autre) ne s'accumulent
 *    jamais ; seul le code réel, maintenu stable, franchit le seuil.
 *
 * Appelé uniquement depuis le listener de succès ML Kit (thread principal), donc pas de
 * synchronisation nécessaire.
 */
private class StableBarcodeConfirmer(
    private val required: Int,
    private val warmupMs: Long
) {
    private var firstSeenAt = 0L
    private var last: String? = null
    private var count = 0

    fun confirm(value: String): Boolean {
        val now = System.currentTimeMillis()
        if (firstSeenAt == 0L) firstSeenAt = now
        // 1. Chauffe : on laisse l'autofocus se stabiliser avant de prendre en compte les lectures
        if (now - firstSeenAt < warmupMs) return false
        // 2. Confirmation : N lectures identiques consécutives
        if (value == last) count++ else { last = value; count = 1 }
        if (count >= required) {
            count = 0
            last = null
            return true
        }
        return false
    }
}

/**
 * Client ML Kit pour l'écran de dépôt : codes-barres 1D **CODE 128 / 39 / 93** (qui conservent la
 * chaîne littérale, zéros de tête inclus) **+ QR code** (RG1 : les CB peuvent inclure le QR généré
 * par IDemat).
 *
 * ⚠️ On exclut volontairement `ITF` et la famille `UPC`/`EAN` : ce sont des symbologies à
 * longueur contrainte (ITF = chiffres par **paires**, UPC/EAN = longueur fixe avec chiffre de
 * contrôle) qui, sur un code numérique comme `000003576`, ajoutent/retirent un `0` de tête ou
 * réinterprètent la valeur. Les laisser activées provoquait des lectures erronées.
 */
private val linearBarcodeScanner by lazy {
    BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeLinearFrame(
    proxy: androidx.camera.core.ImageProxy,
    onDetected: (String, String) -> Unit
) {
    val mediaImage = proxy.image ?: run { proxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    linearBarcodeScanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { value ->
                onDetected(value, barcodes.first().format.toFormatName())
            }
        }
        .addOnCompleteListener { proxy.close() }
}
