package com.example.trafficiq.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.trafficiq.ml.TrackedVehicle
import com.example.trafficiq.ml.TravelDirection
import com.example.trafficiq.ml.TripwireCounter
import kotlin.math.roundToInt

@Composable
fun DetectionCanvasOverlay(
    vehicles: List<TrackedVehicle>,
    tripwireCounter: TripwireCounter,
    recentCrossedTimestamp: Long,
    recentCrossedDirection: TravelDirection?,
    modifier: Modifier = Modifier
) {
    // Pulse animation when vehicle crosses tripwire
    val pulseAnim = remember { Animatable(0f) }

    LaunchedEffect(recentCrossedTimestamp) {
        if (recentCrossedTimestamp > 0) {
            pulseAnim.snapTo(1f)
            pulseAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw Virtual Tripwire
        drawTripwire(
            tripwireCounter = tripwireCounter,
            pulseAlpha = pulseAnim.value,
            width = width,
            height = height
        )

        // 2. Draw Tracked Vehicles (Bounding Boxes, Tracking Trail, Labels)
        for (vehicle in vehicles) {
            drawVehicle(vehicle, width, height)
        }
    }
}

private fun DrawScope.drawTripwire(
    tripwireCounter: TripwireCounter,
    pulseAlpha: Float,
    width: Float,
    height: Float
) {
    val startX = tripwireCounter.lineStart.x * width
    val startY = tripwireCounter.lineStart.y * height
    val endX = tripwireCounter.lineEnd.x * width
    val endY = tripwireCounter.lineEnd.y * height

    // Tripwire glow when triggered
    if (pulseAlpha > 0f) {
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = pulseAlpha * 0.7f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 14f * pulseAlpha
        )
    }

    // Main dashed tripwire line
    drawLine(
        color = Color(0xFFFFD600), // Vibrant Traffic Yellow
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
    )

    // Direction arrow indicators along line
    val midX = (startX + endX) / 2f
    val midY = (startY + endY) / 2f

    drawCircle(
        color = Color(0xFFFFD600),
        radius = 6f,
        center = Offset(midX, midY)
    )

    drawContext.canvas.nativeCanvas.apply {
        val textPaint = Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
        drawText("VIRTUAL COUNTING LINE (DE-DUPLICATED)", startX + 20f, startY - 14f, textPaint)
    }
}

private fun DrawScope.drawVehicle(vehicle: TrackedVehicle, width: Float, height: Float) {
    val left = vehicle.boundingBox.left * width
    val top = vehicle.boundingBox.top * height
    val boxWidth = (vehicle.boundingBox.right - vehicle.boundingBox.left) * width
    val boxHeight = (vehicle.boundingBox.bottom - vehicle.boundingBox.top) * height
    val color = vehicle.type.color

    // 1. Draw Trajectory Trail
    if (vehicle.history.size > 1) {
        for (i in 0 until vehicle.history.size - 1) {
            val p1 = Offset(vehicle.history[i].x * width, vehicle.history[i].y * height)
            val p2 = Offset(vehicle.history[i + 1].x * width, vehicle.history[i + 1].y * height)
            val alpha = (i.toFloat() / vehicle.history.size).coerceIn(0.2f, 0.8f)
            drawLine(
                color = color.copy(alpha = alpha),
                start = p1,
                end = p2,
                strokeWidth = 3f
            )
        }
    }

    // 2. Draw Bounding Box Rectangle
    drawRect(
        color = color.copy(alpha = 0.25f),
        topLeft = Offset(left, top),
        size = Size(boxWidth, boxHeight)
    )
    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(boxWidth, boxHeight),
        style = Stroke(width = 3.5f)
    )

    // Corner brackets for professional AI vision HUD aesthetic
    val bracketLen = (boxWidth * 0.22f).coerceAtMost(30f)
    // Top-Left
    drawLine(color = Color.White, start = Offset(left, top), end = Offset(left + bracketLen, top), strokeWidth = 5f)
    drawLine(color = Color.White, start = Offset(left, top), end = Offset(left, top + bracketLen), strokeWidth = 5f)
    // Top-Right
    drawLine(color = Color.White, start = Offset(left + boxWidth, top), end = Offset(left + boxWidth - bracketLen, top), strokeWidth = 5f)
    drawLine(color = Color.White, start = Offset(left + boxWidth, top), end = Offset(left + boxWidth, top + bracketLen), strokeWidth = 5f)
    // Bottom-Left
    drawLine(color = Color.White, start = Offset(left, top + boxHeight), end = Offset(left + bracketLen, top + boxHeight), strokeWidth = 5f)
    drawLine(color = Color.White, start = Offset(left, top + boxHeight), end = Offset(left, top + boxHeight - bracketLen), strokeWidth = 5f)

    // 3. Draw Centroid dot
    val cx = vehicle.centroid.x * width
    val cy = vehicle.centroid.y * height
    drawCircle(
        color = if (vehicle.hasCrossedTripwire) Color(0xFF00E676) else Color.White,
        radius = 5f,
        center = Offset(cx, cy)
    )

    // 4. Draw Label pill with Native Canvas
    drawContext.canvas.nativeCanvas.apply {
        val labelText = "${vehicle.type.name} #${vehicle.trackId} ${(vehicle.confidence * 100).roundToInt()}%"
        val paint = Paint().apply {
            textSize = 26f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val textWidth = paint.measureText(labelText)
        val pillHeight = 36f

        val bgPaint = Paint().apply {
            this.color = color.toArgb()
            style = Paint.Style.FILL
        }
        drawRoundRect(
            left,
            top - pillHeight,
            left + textWidth + 24f,
            top,
            8f,
            8f,
            bgPaint
        )

        paint.color = android.graphics.Color.BLACK
        drawText(labelText, left + 12f, top - 8f, paint)
    }
}
