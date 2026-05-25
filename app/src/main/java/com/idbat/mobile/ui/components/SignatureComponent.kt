package com.idbat.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.ui.theme.*

@Composable
fun SignatureComponent(
    onValidate: ((ImageBitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var completedStrokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val isEmpty = completedStrokes.isEmpty() && currentStroke.isEmpty()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Signature", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(
                    onClick = { completedStrokes = emptyList(); currentStroke = emptyList() },
                    enabled = !isEmpty
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Effacer",
                        tint = if (isEmpty) Color.LightGray else VeoliaPrincipal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(VeoliaDrawingBg, RoundedCornerShape(12.dp))
                    .border(1.5.dp, VeoliaSubtle, RoundedCornerShape(12.dp))
                    .onSizeChanged { boxSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentStroke = listOf(offset) },
                            onDrag = { change, _ -> currentStroke = currentStroke + change.position },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    completedStrokes = completedStrokes + listOf(currentStroke)
                                    currentStroke = emptyList()
                                }
                            },
                            onDragCancel = { currentStroke = emptyList() }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

                    fun drawStroke(points: List<Offset>) {
                        if (points.isEmpty()) return
                        if (points.size == 1) {
                            drawCircle(VeoliaInk, radius = 2.dp.toPx(), center = points[0])
                            return
                        }
                        val path = Path()
                        path.moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.lastIndex) {
                            val mid = Offset(
                                (points[i].x + points[i + 1].x) / 2f,
                                (points[i].y + points[i + 1].y) / 2f
                            )
                            path.quadraticBezierTo(points[i].x, points[i].y, mid.x, mid.y)
                        }
                        path.lineTo(points.last().x, points.last().y)
                        drawPath(path, VeoliaInk, style = stroke)
                    }

                    completedStrokes.forEach { drawStroke(it) }
                    drawStroke(currentStroke)
                }

                if (isEmpty) {
                    Text(
                        "Signez ici",
                        color = VeoliaPlaceholder,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth(0.75f)
                        .height(1.dp)
                        .background(VeoliaSubtle)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Dessinez votre signature dans le cadre",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                if (onValidate != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val bmp = buildSignatureBitmap(completedStrokes, boxSize, density.density)
                            onValidate(bmp.asImageBitmap())
                        },
                        enabled = !isEmpty,
                        colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}

private fun buildSignatureBitmap(
    strokes: List<List<Offset>>,
    size: IntSize,
    density: Float
): android.graphics.Bitmap {
    val w = size.width.coerceAtLeast(1)
    val h = size.height.coerceAtLeast(1)
    val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(28, 28, 30)
        strokeWidth = 3f * density
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
    }

    strokes.forEach { points ->
        when {
            points.isEmpty() -> return@forEach
            points.size == 1 -> {
                canvas.drawCircle(
                    points[0].x, points[0].y, 2f * density,
                    android.graphics.Paint(paint).apply { style = android.graphics.Paint.Style.FILL_AND_STROKE }
                )
            }
            else -> {
                val path = android.graphics.Path()
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.lastIndex) {
                    val mid = Offset(
                        (points[i].x + points[i + 1].x) / 2f,
                        (points[i].y + points[i + 1].y) / 2f
                    )
                    path.quadTo(points[i].x, points[i].y, mid.x, mid.y)
                }
                path.lineTo(points.last().x, points.last().y)
                canvas.drawPath(path, paint)
            }
        }
    }

    return bitmap
}
