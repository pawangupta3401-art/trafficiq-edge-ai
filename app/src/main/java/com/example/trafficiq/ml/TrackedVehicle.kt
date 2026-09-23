package com.example.trafficiq.ml

import android.graphics.PointF
import android.graphics.RectF

/**
 * Represents a single vehicle detected and tracked over consecutive video frames.
 */
data class TrackedVehicle(
    val trackId: Int,
    val type: VehicleType,
    val confidence: Float,
    val boundingBox: RectF, // Normalized coordinates [0f..1f]
    val centroid: PointF, // (x, y) normalized [0f..1f]
    val history: List<PointF> = listOf(centroid),
    val framesDisappeared: Int = 0,
    val hasCrossedTripwire: Boolean = false,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Compute approximate travel direction in degrees (0 = moving right, 90 = moving down, etc.)
     */
    fun getMovementVector(): PointF? {
        if (history.size < 2) return null
        val start = history.first()
        val current = history.last()
        return PointF(current.x - start.x, current.y - start.y)
    }
}

/**
 * Raw detection output from the inference model prior to tracking association.
 */
data class RawDetection(
    val type: VehicleType,
    val confidence: Float,
    val boundingBox: RectF, // Normalized [0f..1f]
    val label: String
)
