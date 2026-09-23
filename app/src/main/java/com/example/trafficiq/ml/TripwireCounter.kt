package com.example.trafficiq.ml

import android.graphics.PointF

enum class TravelDirection {
    INBOUND,  // Approaching the junction / chowk
    OUTBOUND  // Moving away from the junction / chowk
}

data class VehicleCounts(
    val bikeCount: Int = 0,
    val carCount: Int = 0,
    val busCount: Int = 0,
    val truckCount: Int = 0,
    val inboundCount: Int = 0,
    val outboundCount: Int = 0
) {
    val totalCount: Int get() = bikeCount + carCount + busCount + truckCount

    // Passenger Car Units (PCU) calculation per Indian IRC standards
    val totalPcu: Float get() = (bikeCount * VehicleType.BIKE.pcuFactor) +
            (carCount * VehicleType.CAR.pcuFactor) +
            (busCount * VehicleType.BUS.pcuFactor) +
            (truckCount * VehicleType.TRUCK.pcuFactor)

    fun getCount(type: VehicleType): Int = when (type) {
        VehicleType.BIKE -> bikeCount
        VehicleType.CAR -> carCount
        VehicleType.BUS -> busCount
        VehicleType.TRUCK -> truckCount
    }
}

/**
 * Manages virtual counting line (tripwire) geometry and 1:1 de-duplicated crossing events.
 */
class TripwireCounter(
    var lineStart: PointF = PointF(0.05f, 0.55f),
    var lineEnd: PointF = PointF(0.95f, 0.55f)
) {
    private var counts = VehicleCounts()
    var lastCrossedVehicle: TrackedVehicle? = null
        private set
    var lastCrossedTimestamp: Long = 0
        private set

    fun getCounts(): VehicleCounts = counts

    fun reset() {
        counts = VehicleCounts()
        lastCrossedVehicle = null
        lastCrossedTimestamp = 0
    }

    /**
     * Checks all currently active vehicles against the tripwire.
     * @param vehicles List of currently tracked vehicles
     * @param onCrossed Callback invoked whenever a vehicle crosses the line
     */
    fun processVehicles(
        vehicles: List<TrackedVehicle>,
        tracker: CentroidTracker,
        onCrossed: ((TrackedVehicle, TravelDirection) -> Unit)? = null
    ): VehicleCounts {
        for (vehicle in vehicles) {
            if (vehicle.hasCrossedTripwire) continue
            if (vehicle.history.size < 2) continue

            val pPrev = vehicle.history[vehicle.history.size - 2]
            val pCurr = vehicle.history.last()

            if (segmentsIntersect(pPrev, pCurr, lineStart, lineEnd)) {
                // Determine direction based on y-displacement relative to line
                val direction = if (pCurr.y > pPrev.y) TravelDirection.INBOUND else TravelDirection.OUTBOUND

                tracker.markTripwireCrossed(vehicle.trackId)
                lastCrossedVehicle = vehicle
                lastCrossedTimestamp = System.currentTimeMillis()

                counts = when (vehicle.type) {
                    VehicleType.BIKE -> counts.copy(
                        bikeCount = counts.bikeCount + 1,
                        inboundCount = counts.inboundCount + if (direction == TravelDirection.INBOUND) 1 else 0,
                        outboundCount = counts.outboundCount + if (direction == TravelDirection.OUTBOUND) 1 else 0
                    )
                    VehicleType.CAR -> counts.copy(
                        carCount = counts.carCount + 1,
                        inboundCount = counts.inboundCount + if (direction == TravelDirection.INBOUND) 1 else 0,
                        outboundCount = counts.outboundCount + if (direction == TravelDirection.OUTBOUND) 1 else 0
                    )
                    VehicleType.BUS -> counts.copy(
                        busCount = counts.busCount + 1,
                        inboundCount = counts.inboundCount + if (direction == TravelDirection.INBOUND) 1 else 0,
                        outboundCount = counts.outboundCount + if (direction == TravelDirection.OUTBOUND) 1 else 0
                    )
                    VehicleType.TRUCK -> counts.copy(
                        truckCount = counts.truckCount + 1,
                        inboundCount = counts.inboundCount + if (direction == TravelDirection.INBOUND) 1 else 0,
                        outboundCount = counts.outboundCount + if (direction == TravelDirection.OUTBOUND) 1 else 0
                    )
                }

                onCrossed?.invoke(vehicle, direction)
            }
        }
        return counts
    }

    /**
     * Checks if line segment AB intersects line segment CD.
     */
    private fun segmentsIntersect(a: PointF, b: PointF, c: PointF, d: PointF): Boolean {
        fun ccw(p1: PointF, p2: PointF, p3: PointF): Boolean {
            return (p3.y - p1.y) * (p2.x - p1.x) > (p2.y - p1.y) * (p3.x - p1.x)
        }
        return (ccw(a, c, d) != ccw(b, c, d)) && (ccw(a, b, c) != ccw(a, b, d))
    }
}
