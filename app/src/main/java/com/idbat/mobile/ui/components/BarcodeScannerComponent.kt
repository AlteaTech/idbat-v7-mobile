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
import androidx.compose.material.icons.outlined.QrCodeScanner
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

@Composable
fun BarcodeScannerComponent(
    scannedValue: String?,
    onBarcodeDetected: (value: String, format: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Code-barres / QR Code",
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
                Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(24.dp))
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
                        .height(260.dp)
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
                                            if (isActive) analyzeFrame(proxy) { value, format ->
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

                    // Calque de guidage
                    ScannerOverlay(found = !isActive)

                    if (isActive) {
                        Text(
                            text = "Pointez vers un code-barres ou QR code",
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
private fun ScannerOverlay(found: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val side = size.width * 0.65f
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

        // Coins d'accent
        val len = 20.dp.toPx()
        val sw = 4.dp.toPx()
        listOf(
            Offset(left, top) to Pair(Offset(left, top + len), Offset(left + len, top)),
            Offset(left + side, top) to Pair(Offset(left + side, top + len), Offset(left + side - len, top)),
            Offset(left, top + side) to Pair(Offset(left, top + side - len), Offset(left + len, top + side)),
            Offset(left + side, top + side) to Pair(Offset(left + side, top + side - len), Offset(left + side - len, top + side))
        ).forEach { (_, lines) ->
            drawLine(accentColor, lines.first, lines.second.let { Offset(it.x, lines.first.y + (if (lines.first.y < top + side / 2) len else -len)) }, strokeWidth = sw)
            drawLine(accentColor, lines.second, lines.second.let { Offset(lines.first.x + (if (lines.first.x < left + side / 2) len else -len), it.y) }, strokeWidth = sw)
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permission caméra requise", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
        ) { Text("Autoriser") }
    }
}

private val barcodeScanner by lazy {
    BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417
            )
            .build()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeFrame(
    proxy: androidx.camera.core.ImageProxy,
    onDetected: (String, String) -> Unit
) {
    val mediaImage = proxy.image ?: run { proxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    barcodeScanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { value ->
                onDetected(value, barcodes.first().format.toFormatName())
            }
        }
        .addOnCompleteListener { proxy.close() }
}

