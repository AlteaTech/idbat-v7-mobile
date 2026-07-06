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
                        .height(220.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                ) {
                    val executor = remember { Executors.newSingleThreadExecutor() }
                    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
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
                                                isActive = false
                                                isScanning = false
                                                onBarcodeDetected(value, format)
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
                            text = "Alignez le code-barres dans le cadre",
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
        // Fenêtre rectangulaire large (ratio ~3.2:1) centrée
        val boxWidth = size.width * 0.82f
        val boxHeight = boxWidth / 3.2f
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f
        val corner = CornerRadius(12.dp.toPx())
        val accentColor = if (found) VeoliaSuccess else VeoliaCoral

        drawRect(Color.Black.copy(alpha = 0.45f))
        drawRoundRect(
            Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = corner,
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            accentColor,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = corner,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Ligne de visée horizontale au centre
        drawLine(
            color = accentColor.copy(alpha = 0.9f),
            start = Offset(left + 12.dp.toPx(), top + boxHeight / 2f),
            end = Offset(left + boxWidth - 12.dp.toPx(), top + boxHeight / 2f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/** Client ML Kit restreint aux formats de codes-barres **linéaires (1D)**. */
private val linearBarcodeScanner by lazy {
    BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
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
