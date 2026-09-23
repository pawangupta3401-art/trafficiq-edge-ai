package com.example.trafficiq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trafficiq.data.ChowkInfo
import com.example.trafficiq.data.ChowkPresets
import com.example.trafficiq.theme.DarkBackground
import com.example.trafficiq.theme.DarkBorder
import com.example.trafficiq.theme.DarkSurface
import com.example.trafficiq.theme.DarkSurfaceElevated
import com.example.trafficiq.theme.TextMuted
import com.example.trafficiq.theme.TextPrimary
import com.example.trafficiq.theme.TextSecondary
import com.example.trafficiq.theme.TrafficCyan

@Composable
fun ChowkSelectorModal(
    currentChowkName: String,
    onChowkSelected: (ChowkInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customRoad by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TrafficCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Select Monitoring Chowk",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nagpur Municipal Corporation (NMC) Pilot Chowks",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (showCustomInput) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Chowk / Junction Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrafficCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customRoad,
                        onValueChange = { customRoad = it },
                        label = { Text("Corridor / Road Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrafficCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                onChowkSelected(
                                    ChowkInfo(
                                        name = customName.trim(),
                                        road = if (customRoad.isNotBlank()) customRoad.trim() else "Main Corridor",
                                        zone = "Custom Zone",
                                        typicalPeakPcu = 3000
                                    )
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrafficCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Custom Chowk", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ChowkPresets.NAGPUR_CHOWKS) { chowk ->
                            val isSelected = chowk.name == currentChowkName

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) TrafficCyan else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onChowkSelected(chowk)
                                        onDismiss()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chowk.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TrafficCyan else TextPrimary
                                    )
                                    Text(
                                        text = "${chowk.road} • ${chowk.zone}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = TrafficCyan
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showCustomInput = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TrafficCyan)
                        Text("Add Custom Chowk", color = TrafficCyan)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
