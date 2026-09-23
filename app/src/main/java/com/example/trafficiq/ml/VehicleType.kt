package com.example.trafficiq.ml

import androidx.compose.ui.graphics.Color

/**
 * Standard classification for Indian road traffic monitoring.
 * Addresses MoRTH (Ministry of Road Transport and Highways) 2023 accident study,
 * where Two-wheelers (Bikes) represent 45% of road accident fatalities.
 */
enum class VehicleType(
    val displayName: String,
    val color: Color,
    val cocoLabels: Set<String>,
    val pcuFactor: Float // Passenger Car Unit equivalent for traffic engineering
) {
    BIKE(
        displayName = "Two-Wheeler",
        color = Color(0xFFFF9800), // Vibrant Amber
        cocoLabels = setOf("motorcycle", "bicycle"),
        pcuFactor = 0.5f
    ),
    CAR(
        displayName = "Car / Cab",
        color = Color(0xFF00E5FF), // Electric Cyan
        cocoLabels = setOf("car"),
        pcuFactor = 1.0f
    ),
    BUS(
        displayName = "Bus",
        color = Color(0xFF76FF03), // Lime Green
        cocoLabels = setOf("bus"),
        pcuFactor = 3.0f
    ),
    TRUCK(
        displayName = "Truck",
        color = Color(0xFFFF5252), // Signal Red
        cocoLabels = setOf("truck", "train"),
        pcuFactor = 3.0f
    );

    companion object {
        fun fromLabel(label: String): VehicleType? {
            val normalized = label.trim().lowercase()
            return entries.find { normalized in it.cocoLabels }
        }
    }
}
