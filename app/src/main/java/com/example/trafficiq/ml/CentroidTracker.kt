package com.example.trafficiq.ml

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.hypot

/**
 * Centroid and IoU-based Multi-Object Tracker (MOT) designed for fast on-device edge processing.
 * Associates detections between frames to track individual vehicle trajectories and prevent duplicate counts.
 */
class CentroidTracker(
    private val maxDisappearedFrames: Int = 15,
    private val maxDistanceThreshold: Float = 0.18f // Max normalized distance (18% of frame) between frames
) {
    private var nextObjectId = 1
    private val trackedObjects = mutableMapOf<Int, TrackedVehicle>()

    fun reset() {
        trackedObjects.clear()
        nextObjectId = 1
    }

    /**
     * Updates tracked objects with new raw detections from current frame.
     * @param detections Detections from TFLite detector
     * @return List of currently active tracked vehicles
     */
    @Synchronized
    fun update(detections: List<RawDetection>): List<TrackedVehicle> {
        val now = System.currentTimeMillis()

        // If no detections in current frame
        if (detections.isEmpty()) {
            val toRemove = mutableListOf<Int>()
            for ((id, vehicle) in trackedObjects) {
                val updated = vehicle.copy(framesDisappeared = vehicle.framesDisappeared + 1)
                if (updated.framesDisappeared > maxDisappearedFrames) {
                    toRemove.add(id)
                } else {
                    trackedObjects[id] = updated
                }
            }
            toRemove.forEach { trackedObjects.remove(it) }
            return trackedObjects.values.toList()
        }

        // Compute centroids for new detections
        val inputCentroids = detections.map { det ->
            PointF(
                (det.boundingBox.left + det.boundingBox.right) / 2f,
                (det.boundingBox.top + det.boundingBox.bottom) / 2f
            )
        }

        // If no existing tracked objects, register all as new
        if (trackedObjects.isEmpty()) {
            for (i in detections.indices) {
                register(detections[i], inputCentroids[i], now)
            }
            return trackedObjects.values.toList()
        }

        // Associate existing objects with new detections
        val objectIds = trackedObjects.keys.toList()
        val existingCentroids = objectIds.map { trackedObjects[it]!!.centroid }

        // Compute distance matrix
        val distances = Array(existingCentroids.size) { i ->
            FloatArray(inputCentroids.size) { j ->
                val dx = existingCentroids[i].x - inputCentroids[j].x
                val dy = existingCentroids[i].y - inputCentroids[j].y
                hypot(dx, dy)
            }
        }

        // Greedy matching
        val usedObjects = mutableSetOf<Int>()
        val usedDetections = mutableSetOf<Int>()

        // Flatten distances for sorting
        val pairList = mutableListOf<Triple<Int, Int, Float>>()
        for (i in existingCentroids.indices) {
            for (j in inputCentroids.indices) {
                pairList.add(Triple(i, j, distances[i][j]))
            }
        }
        pairList.sortBy { it.third }

        for ((objIdx, detIdx, dist) in pairList) {
            if (usedObjects.contains(objIdx) || usedDetections.contains(detIdx)) continue
            if (dist > maxDistanceThreshold) continue

            val id = objectIds[objIdx]
            val prev = trackedObjects[id]!!
            val newDet = detections[detIdx]
            val newCentroid = inputCentroids[detIdx]

            val newHistory = (prev.history + newCentroid).takeLast(25) // Keep last 25 points
            trackedObjects[id] = prev.copy(
                boundingBox = newDet.boundingBox,
                centroid = newCentroid,
                confidence = newDet.confidence,
                type = newDet.type, // Update type if re-classified
                history = newHistory,
                framesDisappeared = 0,
                lastSeenTimestamp = now
            )

            usedObjects.add(objIdx)
            usedDetections.add(detIdx)
        }

        // Handle unused existing objects
        val toRemove = mutableListOf<Int>()
        for (i in existingCentroids.indices) {
            if (!usedObjects.contains(i)) {
                val id = objectIds[i]
                val prev = trackedObjects[id]!!
                val updated = prev.copy(framesDisappeared = prev.framesDisappeared + 1)
                if (updated.framesDisappeared > maxDisappearedFrames) {
                    toRemove.add(id)
                } else {
                    trackedObjects[id] = updated
                }
            }
        }
        toRemove.forEach { trackedObjects.remove(it) }

        // Handle unused new detections
        for (j in inputCentroids.indices) {
            if (!usedDetections.contains(j)) {
                register(detections[j], inputCentroids[j], now)
            }
        }

        return trackedObjects.values.toList()
    }

    private fun register(det: RawDetection, centroid: PointF, timestamp: Long) {
        val id = nextObjectId++
        trackedObjects[id] = TrackedVehicle(
            trackId = id,
            type = det.type,
            confidence = det.confidence,
            boundingBox = det.boundingBox,
            centroid = centroid,
            history = listOf(centroid),
            framesDisappeared = 0,
            hasCrossedTripwire = false,
            firstSeenTimestamp = timestamp,
            lastSeenTimestamp = timestamp
        )
    }

    fun markTripwireCrossed(trackId: Int) {
        trackedObjects[trackId]?.let {
            trackedObjects[trackId] = it.copy(hasCrossedTripwire = true)
        }
    }
}
