package com.example.trafficiq.data

import com.example.trafficiq.ml.VehicleCounts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Encapsulates an on-ground traffic monitoring session at a specific chowk/junction.
 */
data class TrafficSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val chowkName: String = "Nagpur - Variety Square",
    val roadName: String = "Mahatma Gandhi Road",
    val city: String = "Nagpur",
    val officerBadge: String = "NTP-402",
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long? = null,
    val counts: VehicleCounts = VehicleCounts(),
    val averageFps: Float = 30.0f,
    val isSynced: Boolean = false
) {
    val durationSeconds: Long
        get() {
            val end = endTimeMillis ?: System.currentTimeMillis()
            return ((end - startTimeMillis) / 1000).coerceAtLeast(1)
        }

    val vehiclesPerMinute: Float
        get() {
            val mins = durationSeconds / 60.0f
            return if (mins > 0f) counts.totalCount / mins else 0f
        }

    fun formattedStartTime(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(startTimeMillis))
    }

    fun formattedDuration(): String {
        val totalSecs = durationSeconds
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
