package com.shatrughna.drivemate.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.data.model.TripReport

@Composable
fun TripReportCard(
    tripReport: TripReport,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Route title & Eco Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Route",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${tripReport.startLocationName} → ${tripReport.endLocationName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF065F46)
                ) {
                    Text(
                        text = "Eco ${tripReport.ecoScore}%",
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Route Map Polyline Visualizer with 3D Depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .graphicsLayer {
                        rotationX = 10f
                        cameraDistance = 14f * density
                    }
                    .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                RoutePolylineCanvas(tripReport = tripReport)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trip Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripStatItem(
                    icon = Icons.Default.DirectionsCar,
                    label = "Distance",
                    value = tripReport.formattedDistance,
                    tint = Color(0xFF38BDF8)
                )
                TripStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = tripReport.formattedDuration,
                    tint = Color(0xFFA78BFA)
                )
                TripStatItem(
                    icon = Icons.Default.Speed,
                    label = "Avg Speed",
                    value = tripReport.formattedAvgSpeed,
                    tint = Color(0xFFFBBF24)
                )
                TripStatItem(
                    icon = Icons.Default.LocalGasStation,
                    label = "Fuel Est.",
                    value = String.format("%.1f L", tripReport.fuelConsumedLiters),
                    tint = Color(0xFF34D399)
                )
            }
        }
    }
}

@Composable
private fun RoutePolylineCanvas(tripReport: TripReport) {
    Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
        val width = size.width
        val height = size.height
        val points = tripReport.routePoints

        if (points.size >= 2) {
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }

            val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
            val lonSpan = (maxLon - minLon).coerceAtLeast(0.0001)

            val padding = 20f
            val drawWidth = width - (padding * 2)
            val drawHeight = height - (padding * 2)

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = padding + ((point.longitude - minLon) / lonSpan * drawWidth).toFloat()
                // Invert Y axis for coordinates
                val y = height - (padding + ((point.latitude - minLat) / latSpan * drawHeight).toFloat())

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw route trace
            drawPath(
                path = path,
                color = Color(0xFF0284C7),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // Start marker (Green)
            val startX = padding + ((points.first().longitude - minLon) / lonSpan * drawWidth).toFloat()
            val startY = height - (padding + ((points.first().latitude - minLat) / latSpan * drawHeight).toFloat())
            drawCircle(color = Color(0xFF10B981), radius = 10f, center = Offset(startX, startY))
            drawCircle(color = Color.White, radius = 5f, center = Offset(startX, startY))

            // End / Parking marker (Red)
            val endX = padding + ((points.last().longitude - minLon) / lonSpan * drawWidth).toFloat()
            val endY = height - (padding + ((points.last().latitude - minLat) / latSpan * drawHeight).toFloat())
            drawCircle(color = Color(0xFFEF4444), radius = 10f, center = Offset(endX, endY))
            drawCircle(color = Color.White, radius = 5f, center = Offset(endX, endY))

        } else {
            // Stylized route graphic when GPS points are simulated/discrete
            val path = Path().apply {
                moveTo(24f, height - 24f)
                cubicTo(width * 0.3f, height * 0.2f, width * 0.7f, height * 0.8f, width - 24f, 24f)
            }
            drawPath(
                path = path,
                color = Color(0xFF0284C7).copy(alpha = 0.5f),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
            // Start point
            drawCircle(color = Color(0xFF10B981), radius = 10f, center = Offset(24f, height - 24f))
            drawCircle(color = Color.White, radius = 5f, center = Offset(24f, height - 24f))

            // End point
            drawCircle(color = Color(0xFFEF4444), radius = 10f, center = Offset(width - 24f, 24f))
            drawCircle(color = Color.White, radius = 5f, center = Offset(width - 24f, 24f))
        }
    }
}

@Composable
private fun TripStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}
