package com.example.trafficiq.ml

import android.graphics.PointF
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripwireCounterTest {

    private lateinit var tripwireCounter: TripwireCounter
    private lateinit var tracker: CentroidTracker

    @Before
    fun setUp() {
        // Horizontal tripwire at y = 0.50
        tripwireCounter = TripwireCounter(
            lineStart = PointF(0.0f, 0.50f),
            lineEnd = PointF(1.0f, 0.50f)
        )
        tracker = CentroidTracker()
    }

    @Test
    fun testVehicleCrossingTripwireIncrementsCount() {
        // Step 1: Vehicle above line (y = 0.40)
        val frame1 = tracker.update(
            listOf(RawDetection(VehicleType.BIKE, 0.9f, RectF(0.4f, 0.35f, 0.5f, 0.45f), "motorcycle"))
        )
        tripwireCounter.processVehicles(frame1, tracker)
        assertEquals(0, tripwireCounter.getCounts().totalCount)

        // Step 2: Vehicle moves across line (y = 0.55) -> Inbound crossing
        val frame2 = tracker.update(
            listOf(RawDetection(VehicleType.BIKE, 0.9f, RectF(0.4f, 0.50f, 0.5f, 0.60f), "motorcycle"))
        )
        var crossedCallbackTriggered = false
        val counts = tripwireCounter.processVehicles(frame2, tracker) { _, dir ->
            crossedCallbackTriggered = true
            assertEquals(TravelDirection.INBOUND, dir)
        }

        assertTrue("Callback must trigger on crossing", crossedCallbackTriggered)
        assertEquals(1, counts.totalCount)
        assertEquals(1, counts.bikeCount)
        assertEquals(1, counts.inboundCount)
        assertEquals(0, counts.outboundCount)
    }

    @Test
    fun testDeduplicationPreventsDoubleCounting() {
        // Step 1: Above line
        val frame1 = tracker.update(
            listOf(RawDetection(VehicleType.CAR, 0.95f, RectF(0.3f, 0.3f, 0.5f, 0.4f), "car"))
        )
        tripwireCounter.processVehicles(frame1, tracker)

        // Step 2: Crosses line
        val frame2 = tracker.update(
            listOf(RawDetection(VehicleType.CAR, 0.95f, RectF(0.3f, 0.55f, 0.5f, 0.65f), "car"))
        )
        tripwireCounter.processVehicles(frame2, tracker)
        assertEquals(1, tripwireCounter.getCounts().totalCount)

        // Step 3: Continues moving below line (must NOT count again!)
        val frame3 = tracker.update(
            listOf(RawDetection(VehicleType.CAR, 0.95f, RectF(0.3f, 0.70f, 0.5f, 0.80f), "car"))
        )
        val counts3 = tripwireCounter.processVehicles(frame3, tracker)
        assertEquals("Vehicle must be counted exactly once", 1, counts3.totalCount)
    }
}
