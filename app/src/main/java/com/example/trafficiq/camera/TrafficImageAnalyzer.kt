package com.example.trafficiq.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.trafficiq.ml.CentroidTracker
import com.example.trafficiq.ml.TrackedVehicle
import com.example.trafficiq.ml.TrafficDetector
import com.example.trafficiq.ml.TravelDirection
import com.example.trafficiq.ml.TripwireCounter
import com.example.trafficiq.ml.VehicleCounts
import java.util.concurrent.atomic.AtomicBoolean

data class AnalyzerFrameResult(
    val trackedVehicles: List<TrackedVehicle>,
    val counts: VehicleCounts,
    val currentFps: Float,
    val isSimulator: Boolean,
    val recentCrossedVehicle: TrackedVehicle? = null,
    val recentCrossedDirection: TravelDirection? = null
)

/**
 * CameraX ImageAnalysis Analyzer running computer vision frame processing.
 */
class TrafficImageAnalyzer(
    private val detector: TrafficDetector,
    private val tracker: CentroidTracker = CentroidTracker(),
    val tripwireCounter: TripwireCounter = TripwireCounter(),
    private val onResult: (AnalyzerFrameResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)
    private var lastFrameTime = System.currentTimeMillis()
    private var frameCount = 0
    private var fps = 30.0f
    var isPaused = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused) {
            imageProxy.close()
            return
        }

        // Drop frame if previous frame is still processing (maintains low latency)
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val startTime = System.currentTimeMillis()

        try {
            // Convert camera frame to Bitmap
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            // 1. Run on-device TFLite Object Detection
            val rawDetections = detector.detect(rotatedBitmap)

            // 2. Run Centroid Multi-Object Tracking
            val trackedVehicles = tracker.update(rawDetections)

            // 3. Process Tripwire crossing & deduplication
            var crossedVehicle: TrackedVehicle? = null
            var crossedDir: TravelDirection? = null

            val updatedCounts = tripwireCounter.processVehicles(trackedVehicles, tracker) { vehicle, direction ->
                crossedVehicle = vehicle
                crossedDir = direction
            }

            // Compute FPS
            frameCount++
            val elapsed = System.currentTimeMillis() - startTime
            val instantFps = if (elapsed > 0) 1000f / elapsed else 30f
            fps = (fps * 0.85f) + (instantFps * 0.15f)

            onResult(
                AnalyzerFrameResult(
                    trackedVehicles = trackedVehicles,
                    counts = updatedCounts,
                    currentFps = fps,
                    isSimulator = detector.isSimulatorActive(),
                    recentCrossedVehicle = crossedVehicle,
                    recentCrossedDirection = crossedDir
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    fun resetTrackerAndCounts() {
        tracker.reset()
        tripwireCounter.reset()
    }
}
