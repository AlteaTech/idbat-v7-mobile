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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PhotoPickerComponent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showSourceDialog by remember { mutableStateOf(false) }
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri.value?.let { photos = photos + it }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { photos = photos + it }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            pendingCameraUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                val uri = createCameraUri(context)
                pendingCameraUri.value = uri
                cameraLauncher.launch(uri)
            }
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
                    imageVector = Icons.Outlined.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Photo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos) { uri ->
                        Box(modifier = Modifier.size(70.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { photos = photos - uri },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Supprimer",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
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
                    .clickable { showSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Prendre une photo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Ajouter une photo") },
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

private fun createCameraUri(context: Context): Uri {
    val file = File(context.cacheDir, "images/photo_${System.currentTimeMillis()}.jpg")
        .also { it.parentFile?.mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
