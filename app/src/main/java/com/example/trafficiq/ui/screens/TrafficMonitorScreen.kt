package com.example.trafficiq.ui.screens

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trafficiq.camera.AnalyzerFrameResult
import com.example.trafficiq.camera.TrafficImageAnalyzer
import com.example.trafficiq.data.TrafficRepository
import com.example.trafficiq.data.TrafficSession
import com.example.trafficiq.ml.CentroidTracker
import com.example.trafficiq.ml.TrackedVehicle
import com.example.trafficiq.ml.TrafficDetector
import com.example.trafficiq.ml.TravelDirection
import com.example.trafficiq.ml.TripwireCounter
import com.example.trafficiq.ml.VehicleCounts
import com.example.trafficiq.theme.DarkBackground
import com.example.trafficiq.theme.DarkBorder
import com.example.trafficiq.theme.DarkSurface
import com.example.trafficiq.theme.GlassSurface
import com.example.trafficiq.theme.TextMuted
import com.example.trafficiq.theme.TextPrimary
import com.example.trafficiq.theme.TrafficAmber
import com.example.trafficiq.theme.TrafficCyan
import com.example.trafficiq.theme.TrafficEmerald
import com.example.trafficiq.ui.components.ChowkSelectorModal
import com.example.trafficiq.ui.components.DetectionCanvasOverlay
import com.example.trafficiq.ui.components.LiveAnalyticsBar
import com.example.trafficiq.ui.components.SessionSummaryDialog
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun TrafficMonitorScreen(
    repository: TrafficRepository,
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Session State
    val currentSession by repository.currentSession.collectAsState()
    var sessionDuration by remember { mutableStateOf("00:00") }

    // Live AI Vision States
    var trackedVehicles by remember { mutableStateOf<List<TrackedVehicle>>(emptyList()) }
    var liveCounts by remember { mutableStateOf(VehicleCounts()) }
    var liveFps by remember { mutableFloatStateOf(30f) }
    var isSimulator by remember { mutableStateOf(false) }
    var lastCrossedTime by remember { mutableLongStateOf(0L) }
    var lastCrossedDir by remember { mutableStateOf<TravelDirection?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    // Dialogs
    var showChowkSelector by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var completedSessionForDialog by remember { mutableStateOf<TrafficSession?>(null) }

    // Core Vision Components
    val detector = remember { TrafficDetector(context) }
    val tracker = remember { CentroidTracker() }
    val tripwireCounter = remember { TripwireCounter() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val analyzer = remember {
        TrafficImageAnalyzer(
            detector = detector,
            tracker = tracker,
            tripwireCounter = tripwireCounter
        ) { result: AnalyzerFrameResult ->
            trackedVehicles = result.trackedVehicles
            liveCounts = result.counts
            liveFps = result.currentFps
            isSimulator = result.isSimulator
            if (result.recentCrossedVehicle != null) {
                lastCrossedTime = System.currentTimeMillis()
                lastCrossedDir = result.recentCrossedDirection
            }
            repository.updateLiveCounts(result.counts, result.currentFps)
        }
    }

    // Timer loop for active session duration
    LaunchedEffect(currentSession.startTimeMillis) {
        while (true) {
            sessionDuration = currentSession.formattedDuration()
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            detector.close()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        // 1. Live Camera Preview (CameraX)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, analyzer)
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Detection Canvas Overlay (Boxes, Labels, Tripwire, Trails)
        DetectionCanvasOverlay(
            vehicles = trackedVehicles,
            tripwireCounter = tripwireCounter,
            recentCrossedTimestamp = lastCrossedTime,
            recentCrossedDirection = lastCrossedDir,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top HUD: Chowk Title, Session Timer, Live FPS, Simulator Badge
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 36.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chowk info (clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showChowkSelector = true }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) TrafficAmber else TrafficEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentSession.chowkName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.EditLocation,
                                contentDescription = "Change Chowk",
                                tint = TrafficCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${currentSession.roadName} • ${currentSession.officerBadge}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // FPS & Timer Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${liveFps.toInt()} FPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (liveFps >= 24) TrafficEmerald else TrafficAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = sessionDuration,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Fallback simulator notification if active
            if (isSimulator) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD90F172A))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⚡ EDGE SIMULATOR ACTIVE • 45% 2-Wheeler Nagpur Traffic Mix",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // 4. Quick Action Controls (Floating along right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pause / Resume Toggle
            IconButton(
                onClick = {
                    isPaused = !isPaused
                    analyzer.isPaused = isPaused
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, DarkBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = if (isPaused) TrafficEmerald else TrafficAmber
                )
            }

            // Reset Counts
            IconButton(
                onClick = {
                    analyzer.resetTrackerAndCounts()
                    liveCounts = VehicleCounts()
                    trackedVehicles = emptyList()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, DarkBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = TextPrimary
                )
            }

            // Adjust Tripwire Line (toggle between middle 55% and 65% position)
            IconButton(
                onClick = {
                    val currentY = tripwireCounter.lineStart.y
                    val newY = if (currentY >= 0.60f) 0.50f else 0.65f
                    tripwireCounter.lineStart.y = newY
                    tripwireCounter.lineEnd.y = newY
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, DarkBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Adjust Tripwire",
                    tint = TrafficCyan
                )
            }

            // Save / Sync Session
            IconButton(
                onClick = {
                    val completed = repository.completeAndSaveSession()
                    completedSessionForDialog = completed
                    showSummaryDialog = true
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TrafficCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Complete & Sync",
                    tint = DarkBackground
                )
            }

            // View Session History Logs
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, DarkBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Session History",
                    tint = TextPrimary
                )
            }
        }

        // 5. Bottom Live Analytics Dashboard Bar
        LiveAnalyticsBar(
            counts = liveCounts,
            vehiclesPerMinute = currentSession.vehiclesPerMinute,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Chowk Selector Modal
    if (showChowkSelector) {
        ChowkSelectorModal(
            currentChowkName = currentSession.chowkName,
            onChowkSelected = { newChowk ->
                repository.updateChowk(newChowk)
                showChowkSelector = false
            },
            onDismiss = { showChowkSelector = false }
        )
    }

    // Session Summary & Cloud Sync Modal
    if (showSummaryDialog && completedSessionForDialog != null) {
        val session = completedSessionForDialog!!
        SessionSummaryDialog(
            session = session,
            csvContent = repository.exportToCsv(session),
            jsonPayload = repository.buildSyncPayload(session),
            onSyncComplete = {
                repository.markSessionSynced(session.sessionId)
            },
            onDismiss = {
                showSummaryDialog = false
                completedSessionForDialog = null
            }
        )
    }
}
