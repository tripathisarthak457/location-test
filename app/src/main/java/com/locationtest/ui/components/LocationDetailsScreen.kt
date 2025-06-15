package com.locationtest.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.locationtest.model.LocationData
import com.locationtest.utils.LocationUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailsScreen(
    locationData: LocationData,
    connectionType: String,
    modifier: Modifier = Modifier
) {
    // Animation for the entire content
    val slideAnimation = remember { Animatable(0f) }
    
    LaunchedEffect(locationData) {
        slideAnimation.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Header Card
                AnimatedVisibility(
                    visible = slideAnimation.value > 0f,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(600, delayMillis = 100)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 100))
                ) {
                    HeaderCard(locationData = locationData)
                }
            }

            item {
                // GPS Metrics Card
                AnimatedVisibility(
                    visible = slideAnimation.value > 0f,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(600, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                ) {
                    GPSMetricsCard(locationData = locationData)
                }
            }

            item {
                // Connection Info Card
                AnimatedVisibility(
                    visible = slideAnimation.value > 0f,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(600, delayMillis = 300)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
                ) {
                    ConnectionInfoCard(connectionType = connectionType)
                }
            }

            item {
                // Technical Details Card
                AnimatedVisibility(
                    visible = slideAnimation.value > 0f,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(600, delayMillis = 400)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                ) {
                    TechnicalDetailsCard(locationData = locationData)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(locationData: LocationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "📍 Current Location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault())
            Text(
                text = dateFormat.format(Date(locationData.timestamp)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GPSMetricsCard(locationData: LocationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🛰️ GPS Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MetricRow(
                icon = Icons.Default.LocationOn,
                label = "Coordinates",
                value = "${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}",
                color = MaterialTheme.colorScheme.primary
            )
            
            locationData.accuracy?.let { accuracy ->
                MetricRow(
                    icon = Icons.Default.LocationOn,
                    label = "Accuracy",
                    value = LocationUtils.getAccuracyDescription(accuracy),
                    color = when {
                        accuracy <= 5 -> Color(0xFF4CAF50)
                        accuracy <= 10 -> Color(0xFF8BC34A)
                        accuracy <= 50 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            
            locationData.altitude?.let { altitude ->
                MetricRow(
                    icon = Icons.Default.KeyboardArrowUp,
                    label = "Altitude",
                    value = "${String.format("%.1f", altitude)} m",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            locationData.speed?.let { speed ->
                MetricRow(
                    icon = Icons.Default.PlayArrow,
                    label = "Speed",
                    value = LocationUtils.getSpeedInKmh(speed),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            locationData.bearing?.let { bearing ->
                MetricRow(
                    icon = Icons.Default.Place,
                    label = "Bearing",
                    value = "${String.format("%.1f", bearing)}°",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ConnectionInfoCard(connectionType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🌐 Connection Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MetricRow(
                icon = Icons.Default.Settings,
                label = "Connection Type",
                value = connectionType,
                color = MaterialTheme.colorScheme.primary
            )
            
            MetricRow(
                icon = Icons.Default.CheckCircle,
                label = "Status",
                value = "Connected",
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun TechnicalDetailsCard(locationData: LocationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🔧 Technical Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MetricRow(
                icon = Icons.Default.Info,
                label = "Provider",
                value = locationData.provider ?: "Unknown",
                color = MaterialTheme.colorScheme.secondary
            )
            
            MetricRow(
                icon = Icons.Default.DateRange,
                label = "Timestamp",
                value = locationData.timestamp.toString(),
                color = MaterialTheme.colorScheme.tertiary
            )
            
            MetricRow(
                icon = Icons.Default.Star,
                label = "Location ID",
                value = locationData.id.takeLast(8),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
