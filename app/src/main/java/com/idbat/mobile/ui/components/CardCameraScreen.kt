package com.idbat.mobile.ui.components

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CropLandscape
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.io.File

@Composable
fun CardCameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isVertical by remember { mutableStateOf(false) }
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Preview caméra
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCaptureRef.value = capture
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Calque de guidage
        CardGuideOverlay(isVertical = isVertical)

        // Barre du haut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            // Toggle mode
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    ModeButton(
                        icon = Icons.Outlined.CropLandscape,
                        label = "Ligne",
                        selected = !isVertical,
                        onClick = { isVertical = false }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ModeButton(
                        icon = Icons.Outlined.CropPortrait,
                        label = "Vertical",
                        selected = isVertical,
                        onClick = { isVertical = true }
                    )
                }
            }
        }

        // Bas : hint + déclencheur
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isVertical) "Cadrez le numéro vertical dans le guide"
                       else "Cadrez la face de la carte dans le guide",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
            ShutterButton(
                onClick = {
                    imageCaptureRef.value?.let { capture ->
                        captureCardPhoto(capture, context, onPhotoCaptured)
                    }
                }
            )
        }
    }
}

@Composable
private fun CardGuideOverlay(isVertical: Boolean) {
    val cardRatio = 85.6f / 53.98f // ratio standard d'une carte bancaire

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val (cardW, cardH) = if (isVertical) {
            val h = size.height * 0.58f
            Pair(h / cardRatio, h)
        } else {
            val w = size.width * 0.85f
            Pair(w, w / cardRatio)
        }

        val left = (size.width - cardW) / 2f
        val top = (size.height - cardH) / 2f - 30.dp.toPx()
        val corner = CornerRadius(14.dp.toPx())

        // Fond sombre
        drawRect(Color.Black.copy(alpha = 0.55f))

        // Découpe transparente (fenêtre de cadrage)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(cardW, cardH),
            cornerRadius = corner,
            blendMode = BlendMode.Clear
        )

        // Bordure blanche
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(cardW, cardH),
            cornerRadius = corner,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Coins d'accent (orange)
        val accentColor = androidx.compose.ui.graphics.Color(0xFFF27059)
        val accentLen = 24.dp.toPx()
        val strokeW = Stroke(width = 4.dp.toPx())
        // top-left
        drawLine(accentColor, Offset(left, top + accentLen), Offset(left, top + corner.x), strokeWidth = 4.dp.toPx())
        drawLine(accentColor, Offset(left + corner.x, top), Offset(left + accentLen, top), strokeWidth = 4.dp.toPx())
        // top-right
        drawLine(accentColor, Offset(left + cardW, top + accentLen), Offset(left + cardW, top + corner.x), strokeWidth = 4.dp.toPx())
        drawLine(accentColor, Offset(left + cardW - corner.x, top), Offset(left + cardW - accentLen, top), strokeWidth = 4.dp.toPx())
        // bottom-left
        drawLine(accentColor, Offset(left, top + cardH - accentLen), Offset(left, top + cardH - corner.x), strokeWidth = 4.dp.toPx())
        drawLine(accentColor, Offset(left + corner.x, top + cardH), Offset(left + accentLen, top + cardH), strokeWidth = 4.dp.toPx())
        // bottom-right
        drawLine(accentColor, Offset(left + cardW, top + cardH - accentLen), Offset(left + cardW, top + cardH - corner.x), strokeWidth = 4.dp.toPx())
        drawLine(accentColor, Offset(left + cardW - corner.x, top + cardH), Offset(left + cardW - accentLen, top + cardH), strokeWidth = 4.dp.toPx())
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White, CircleShape)
        )
    }
}

private fun captureCardPhoto(
    imageCapture: ImageCapture,
    context: Context,
    onResult: (Uri) -> Unit
) {
    val file = File(context.cacheDir, "images/cb_${System.currentTimeMillis()}.jpg")
        .also { it.parentFile?.mkdirs() }
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = output.savedUri
                    ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                onResult(uri)
            }
            override fun onError(exc: ImageCaptureException) {}
        }
    )
}
