package com.example.trafficiq.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

/**
 * On-device TensorFlow Lite Object Detection engine optimized for edge mobile inference.
 * Reads camera frames, applies vehicle classification, and emits normalized bounding boxes.
 */
class TrafficDetector(
    private val context: Context,
    private val modelFileName: String = "ssd_mobilenet_v2_coco_quant.tflite",
    private val labelFileName: String = "coco_labels.txt",
    private val confidenceThreshold: Float = 0.45f
) {
    companion object {
        private const val TAG = "TrafficDetector"
        private const val INPUT_SIZE = 300
        private const val NUM_DETECTIONS = 10
    }

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    private var isSimulatedFallback = false
    private val random = Random()

    // Synthetic traffic simulation state for preview/testing without model binary
    private val simulatedVehicles = mutableListOf<SimulatedVehicle>()

    init {
        loadLabels()
        setupInterpreter()
    }

    private fun loadLabels() {
        try {
            context.assets.open(labelFileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            labels.add(trimmed)
                        }
                    }
                }
            }
            Log.d(TAG, "Loaded ${labels.size} labels from $labelFileName")
        } catch (e: Exception) {
            Log.w(TAG, "Could not load labels file: ${e.message}. Using built-in vehicle labels.")
            labels.addAll(listOf("person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck"))
        }
    }

    private fun setupInterpreter() {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, modelFileName)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }
            interpreter = Interpreter(modelBuffer, options)
            isSimulatedFallback = false
            Log.i(TAG, "TensorFlow Lite interpreter initialized successfully with $modelFileName")
        } catch (e: Exception) {
            Log.w(TAG, "TFLite model binary '$modelFileName' not found or failed to load. Enabling edge traffic simulator fallback: ${e.message}")
            isSimulatedFallback = true
            initSimulationVehicles()
        }
    }

    /**
     * Detects vehicles in the provided bitmap image.
     */
    fun detect(bitmap: Bitmap): List<RawDetection> {
        return if (isSimulatedFallback || interpreter == null) {
            detectSimulated(bitmap.width, bitmap.height)
        } else {
            detectTflite(bitmap)
        }
    }

    private fun detectTflite(bitmap: Bitmap): List<RawDetection> {
        val interp = interpreter ?: return emptyList()

        // Resize bitmap to model input size
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(intValues, 0, resized.width, 0, 0, resized.width, resized.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]
                byteBuffer.put(((`val` shr 16) and 0xFF).toByte())
                byteBuffer.put(((`val` shr 8) and 0xFF).toByte())
                byteBuffer.put((`val` and 0xFF).toByte())
            }
        }

        // TFLite SSD MobileNet output structures
        val outputLocations = Array(1) { Array(NUM_DETECTIONS) { FloatArray(4) } }
        val outputClasses = Array(1) { FloatArray(NUM_DETECTIONS) }
        val outputScores = Array(1) { FloatArray(NUM_DETECTIONS) }
        val numDetections = FloatArray(1)

        val outputMap = HashMap<Int, Any>()
        outputMap[0] = outputLocations
        outputMap[1] = outputClasses
        outputMap[2] = outputScores
        outputMap[3] = numDetections

        interp.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        val detections = mutableListOf<RawDetection>()
        val count = numDetections[0].toInt().coerceAtMost(NUM_DETECTIONS)

        for (i in 0 until count) {
            val score = outputScores[0][i]
            if (score < confidenceThreshold) continue

            val classIndex = outputClasses[0][i].toInt()
            val label = if (classIndex in labels.indices) labels[classIndex] else "unknown"

            // Filter for target vehicle classes
            val vehicleType = VehicleType.fromLabel(label) ?: continue

            // Output format: [top, left, bottom, right]
            val top = outputLocations[0][i][0].coerceIn(0f, 1f)
            val left = outputLocations[0][i][1].coerceIn(0f, 1f)
            val bottom = outputLocations[0][i][2].coerceIn(0f, 1f)
            val right = outputLocations[0][i][3].coerceIn(0f, 1f)

            if (bottom > top && right > left) {
                detections.add(
                    RawDetection(
                        type = vehicleType,
                        confidence = score,
                        boundingBox = RectF(left, top, right, bottom),
                        label = label
                    )
                )
            }
        }

        return detections
    }

    /**
     * High-fidelity synthetic Indian traffic generator for dev preview, unit testing,
     * or when TFLite model is not yet compiled onto device assets.
     */
    private fun initSimulationVehicles() {
        simulatedVehicles.clear()
        // Generate realistic Indian road mix (45% bikes, 35% cars, 10% buses, 10% trucks)
        simulatedVehicles.add(SimulatedVehicle(VehicleType.BIKE, 0.20f, 0.15f, 0.08f, 0.10f, 0.007f))
        simulatedVehicles.add(SimulatedVehicle(VehicleType.CAR, 0.45f, 0.30f, 0.18f, 0.16f, 0.005f))
        simulatedVehicles.add(SimulatedVehicle(VehicleType.BIKE, 0.70f, 0.05f, 0.07f, 0.09f, 0.008f))
        simulatedVehicles.add(SimulatedVehicle(VehicleType.BUS, 0.35f, 0.65f, 0.24f, 0.22f, 0.004f))
    }

    private fun detectSimulated(frameWidth: Int, frameHeight: Int): List<RawDetection> {
        val results = mutableListOf<RawDetection>()

        for (sim in simulatedVehicles) {
            sim.y += sim.speedY
            if (sim.y > 1.05f) {
                // Respawn at top with randomized lane & vehicle type reflecting Indian distribution
                sim.y = -0.15f
                sim.x = 0.10f + random.nextFloat() * 0.75f
                val roll = random.nextFloat()
                sim.type = when {
                    roll < 0.48f -> VehicleType.BIKE // 48% Two-wheelers
                    roll < 0.82f -> VehicleType.CAR  // 34% Cars
                    roll < 0.92f -> VehicleType.BUS  // 10% Buses
                    else -> VehicleType.TRUCK         // 8% Trucks
                }
                when (sim.type) {
                    VehicleType.BIKE -> { sim.width = 0.08f; sim.height = 0.10f; sim.speedY = 0.007f + random.nextFloat() * 0.004f }
                    VehicleType.CAR -> { sim.width = 0.16f; sim.height = 0.14f; sim.speedY = 0.005f + random.nextFloat() * 0.003f }
                    VehicleType.BUS -> { sim.width = 0.22f; sim.height = 0.22f; sim.speedY = 0.0035f }
                    VehicleType.TRUCK -> { sim.width = 0.24f; sim.height = 0.24f; sim.speedY = 0.0032f }
                }
            }

            // Only output if visible in frame
            if (sim.y in -0.1f..1.1f) {
                val left = (sim.x - sim.width / 2).coerceIn(0f, 1f)
                val top = (sim.y - sim.height / 2).coerceIn(0f, 1f)
                val right = (sim.x + sim.width / 2).coerceIn(0f, 1f)
                val bottom = (sim.y + sim.height / 2).coerceIn(0f, 1f)

                if (right > left && bottom > top) {
                    results.add(
                        RawDetection(
                            type = sim.type,
                            confidence = 0.85f + (random.nextFloat() * 0.12f),
                            boundingBox = RectF(left, top, right, bottom),
                            label = sim.type.name.lowercase()
                        )
                    )
                }
            }
        }

        return results
    }

    fun isSimulatorActive(): Boolean = isSimulatedFallback

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private class SimulatedVehicle(
        var type: VehicleType,
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var speedY: Float
    )
}
