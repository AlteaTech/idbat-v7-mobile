package com.idbat.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.idbat.mobile.ui.theme.VeoliaPrincipal

@Composable
fun BottomToast(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // En-tête avec titre et bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )

                    // Bouton fermer circulaire
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(VeoliaPrincipal)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Contenu avec texte formaté
                Text(
                    text = formatContent(content),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun formatContent(content: String) = buildAnnotatedString {
    val lines = content.split("\n")
    lines.forEachIndexed { index, line ->
        when {
            line.contains("Dernier envoi réussi le:") ||
                    line.contains("Dernière réception réussie le:") ||
                    line.contains("Opérations:") ||
                    line.contains("Opérations non transférées:") -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Black)) {
                    append(line)
                }
            }
            else -> {
                append(line)
            }
        }
        if (index < lines.size - 1) append("\n")
    }
}

@Composable
fun ToastHost(
    toastState: ToastState
) {
    toastState.currentToast?.let { toast ->
        BottomToast(
            title = toast.title,
            content = toast.content,
            onDismiss = { toastState.dismiss() }
        )
    }
}

class ToastState {
    private var _currentToast by mutableStateOf<ToastData?>(null)
    val currentToast: ToastData? get() = _currentToast

    fun showToast(title: String, content: String) {
        _currentToast = ToastData(title, content)
    }

    fun dismiss() {
        _currentToast = null
    }
}

data class ToastData(
    val title: String,
    val content: String
)

@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}