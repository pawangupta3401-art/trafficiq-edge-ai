package com.example.trafficiq.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trafficiq.ml.VehicleCounts
import com.example.trafficiq.ml.VehicleType
import com.example.trafficiq.theme.DarkBorder
import com.example.trafficiq.theme.DarkSurface
import com.example.trafficiq.theme.GlassSurface
import com.example.trafficiq.theme.TextMuted
import com.example.trafficiq.theme.TextPrimary
import com.example.trafficiq.theme.TextSecondary
import com.example.trafficiq.theme.TrafficCyan
import com.example.trafficiq.theme.TrafficEmerald

@Composable
fun LiveAnalyticsBar(
    counts: VehicleCounts,
    vehiclesPerMinute: Float,
    modifier: Modifier = Modifier
) {
    val total = counts.totalCount.coerceAtLeast(1)
    val bikePct = (counts.bikeCount.toFloat() / total * 100).toInt()
    val carPct = (counts.carCount.toFloat() / total * 100).toInt()
    val busPct = (counts.busCount.toFloat() / total * 100).toInt()
    val truckPct = (counts.truckCount.toFloat() / total * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(GlassSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
    ) {
        // Top Stats Row: Total, Flow Rate, PCU
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL VEHICLES COUNTED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                AnimatedContent(
                    targetState = counts.totalCount,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "totalCount"
                ) { value ->
                    Text(
                        text = "$value",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricPill(
                    label = "FLOW RATE",
                    value = String.format("%.1f", vehiclesPerMinute),
                    unit = "v/min",
                    color = TrafficEmerald
                )
                MetricPill(
                    label = "PCU LOAD",
                    value = String.format("%.1f", counts.totalPcu),
                    unit = "eq",
                    color = TrafficCyan
                )
                MetricPill(
                    label = "IN / OUT",
                    value = "${counts.inboundCount}/${counts.outboundCount}",
                    unit = "",
                    color = Color(0xFFFFD600)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Distribution Proportional Bar
        if (counts.totalCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
            ) {
                if (counts.bikeCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(counts.bikeCount.toFloat())
                            .height(8.dp)
                            .background(VehicleType.BIKE.color)
                    )
                }
                if (counts.carCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(counts.carCount.toFloat())
                            .height(8.dp)
                            .background(VehicleType.CAR.color)
                    )
                }
                if (counts.busCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(counts.busCount.toFloat())
                            .height(8.dp)
                            .background(VehicleType.BUS.color)
                    )
                }
                if (counts.truckCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(counts.truckCount.toFloat())
                            .height(8.dp)
                            .background(VehicleType.TRUCK.color)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Vehicle Class Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VehicleClassCard(
                type = VehicleType.BIKE,
                count = counts.bikeCount,
                percent = bikePct,
                badge = "45% Risk",
                modifier = Modifier.weight(1f)
            )
            VehicleClassCard(
                type = VehicleType.CAR,
                count = counts.carCount,
                percent = carPct,
                modifier = Modifier.weight(1f)
            )
            VehicleClassCard(
                type = VehicleType.BUS,
                count = counts.busCount,
                percent = busPct,
                modifier = Modifier.weight(1f)
            )
            VehicleClassCard(
                type = VehicleType.TRUCK,
                count = counts.truckCount,
                percent = truckPct,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun VehicleClassCard(
    type: VehicleType,
    count: Int,
    percent: Int,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, type.color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = type.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = type.color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "$percent%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
