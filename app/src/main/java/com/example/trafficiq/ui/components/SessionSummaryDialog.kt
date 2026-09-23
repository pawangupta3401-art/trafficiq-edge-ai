package com.example.trafficiq.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trafficiq.data.TrafficSession
import com.example.trafficiq.ml.VehicleType
import com.example.trafficiq.theme.DarkBackground
import com.example.trafficiq.theme.DarkBorder
import com.example.trafficiq.theme.DarkSurface
import com.example.trafficiq.theme.DarkSurfaceElevated
import com.example.trafficiq.theme.TextMuted
import com.example.trafficiq.theme.TextPrimary
import com.example.trafficiq.theme.TextSecondary
import com.example.trafficiq.theme.TrafficCyan
import com.example.trafficiq.theme.TrafficEmerald
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SessionSummaryDialog(
    session: TrafficSession,
    csvContent: String,
    jsonPayload: String,
    onSyncComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncSuccess by remember { mutableStateOf(session.isSynced) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                Text(
                    text = "Session Summary & Sync",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${session.chowkName} (${session.formattedDuration()})",
                    fontSize = 12.sp,
                    color = TrafficCyan
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Key metrics box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Vehicles", color = TextSecondary, fontSize = 13.sp)
                            Text("${session.counts.totalCount}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Two-Wheelers (Bikes)", color = VehicleType.BIKE.color, fontSize = 13.sp)
                            Text("${session.counts.bikeCount}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cars / Cabs", color = VehicleType.CAR.color, fontSize = 13.sp)
                            Text("${session.counts.carCount}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Buses", color = VehicleType.BUS.color, fontSize = 13.sp)
                            Text("${session.counts.busCount}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trucks", color = VehicleType.TRUCK.color, fontSize = 13.sp)
                            Text("${session.counts.truckCount}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Avg Flow Rate", color = TextMuted, fontSize = 12.sp)
                            Text("${String.format("%.1f", session.vehiclesPerMinute)} v/min", color = TrafficEmerald, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sync status indicator
                if (syncSuccess) {
                    Text(
                        text = "✓ Synced with Nagpur Smart City Central Server",
                        color = TrafficEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "• Summary counts ready for periodic transmission (Edge offline mode)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Sync button
                Button(
                    onClick = {
                        isSyncing = true
                        coroutineScope.launch {
                            delay(1200) // Simulate fast network transmission of lightweight count JSON
                            isSyncing = false
                            syncSuccess = true
                            onSyncComplete()
                            Toast.makeText(context, "Telemetry synced to Central Dashboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSyncing && !syncSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = TrafficCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = DarkBackground,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp).width(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transmitting Telemetry...", color = DarkBackground)
                    } else {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (syncSuccess) "Dashboard Synced" else "Sync to Smart City Central",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Copy CSV button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TrafficIQ CSV", csvContent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Session CSV copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy CSV Export (For Planners)", color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TrafficCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
