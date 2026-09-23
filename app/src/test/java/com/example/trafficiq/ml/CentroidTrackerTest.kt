package com.example.trafficiq.ml

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CentroidTrackerTest {

    private lateinit var tracker: CentroidTracker

    @Before
    fun setUp() {
        tracker = CentroidTracker(maxDisappearedFrames = 5, maxDistanceThreshold = 0.20f)
    }

    @Test
    fun testRegisterNewVehicle() {
        val detection = RawDetection(
            type = VehicleType.BIKE,
            confidence = 0.92f,
            boundingBox = RectF(0.2f, 0.2f, 0.3f, 0.3f),
            label = "motorcycle"
        )

        val tracked = tracker.update(listOf(detection))
        assertEquals(1, tracked.size)
        assertEquals(VehicleType.BIKE, tracked[0].type)
        assertEquals(1, tracked[0].trackId)
    }

    @Test
    fun testTrackVehicleAcrossConsecutiveFrames() {
        val frame1 = listOf(
            RawDetection(VehicleType.CAR, 0.88f, RectF(0.4f, 0.2f, 0.6f, 0.4f), "car")
        )
        val tracked1 = tracker.update(frame1)
        val initialId = tracked1[0].trackId

        // Vehicle moves down slightly in next frame
        val frame2 = listOf(
            RawDetection(VehicleType.CAR, 0.90f, RectF(0.4f, 0.25f, 0.6f, 0.45f), "car")
        )
        val tracked2 = tracker.update(frame2)

        assertEquals(1, tracked2.size)
        assertEquals("Track ID must persist across consecutive frames", initialId, tracked2[0].trackId)
        assertEquals(2, tracked2[0].history.size)
    }

    @Test
    fun testVehicleDeregistrationWhenExitingFrame() {
        val frame1 = listOf(
            RawDetection(VehicleType.BUS, 0.85f, RectF(0.3f, 0.3f, 0.6f, 0.6f), "bus")
        )
        tracker.update(frame1)

        // Vehicle disappears for more than maxDisappearedFrames (5)
        for (i in 0 until 6) {
            tracker.update(emptyList())
        }

        val trackedAfterExit = tracker.update(emptyList())
        assertTrue("Vehicle must be deregistered after exceeding max disappeared frames", trackedAfterExit.isEmpty())
    }
}
