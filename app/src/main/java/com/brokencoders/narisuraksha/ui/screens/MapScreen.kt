package com.brokencoders.narisuraksha.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brokencoders.narisuraksha.R
import com.brokencoders.narisuraksha.data.SafeZoneCategory
import com.brokencoders.narisuraksha.data.SafeZoneItem
import com.brokencoders.narisuraksha.data.SafeZonesRepository
import com.brokencoders.narisuraksha.ui.components.AppBottomBar
import com.brokencoders.narisuraksha.ui.components.InfoCard
import com.brokencoders.narisuraksha.ui.components.SecondaryButton
import com.brokencoders.narisuraksha.ui.theme.BorderDivider
import com.brokencoders.narisuraksha.ui.theme.CardBackground
import com.brokencoders.narisuraksha.ui.theme.EmergencyRed
import com.brokencoders.narisuraksha.ui.theme.GuardianBlue
import com.brokencoders.narisuraksha.ui.theme.PrimaryPink
import com.brokencoders.narisuraksha.ui.theme.SafeGreen
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurface
import com.brokencoders.narisuraksha.ui.theme.TextPrimary
import com.brokencoders.narisuraksha.ui.theme.TextSecondary
import com.brokencoders.narisuraksha.ui.theme.VigilanceAmber
import com.brokencoders.narisuraksha.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentCoords by viewModel.currentCoordinates.collectAsState()
    val latestAlert by viewModel.latestReceivedAlert.collectAsState()

    var selectedFilter by remember { mutableStateOf<SafeZoneCategory?>(null) }
    var selectedSafeZone by remember { mutableStateOf<SafeZoneItem?>(null) }

    val userLat = currentCoords?.lat ?: 28.6139
    val userLon = currentCoords?.lon ?: 77.2090
    val isGpsActive = currentCoords != null

    val safeZones = remember(currentCoords) {
        SafeZonesRepository.preCachedSafeZones.sortedBy { zone ->
            SafeZonesRepository.calculateDistanceKm(userLat, userLon, zone.lat, zone.lon)
        }
    }

    val filteredZones = remember(safeZones, selectedFilter) {
        if (selectedFilter == null) safeZones else safeZones.filter { it.category == selectedFilter }
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoftPinkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.tactical_radar_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshLocation() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh GPS",
                            tint = PrimaryPink,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "map",
                onNavigate = { route ->
                    if (route != "map") onBack()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 250.dp,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetDragHandle = { BottomSheetDefaults.DragHandle() },
            sheetContent = {
                // Bottom sheet of nearest safe zones with 1-tap call button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nearest_safe_zones),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafeGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "100% OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All (${safeZones.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPink,
                                selectedLabelColor = Color.White,
                                containerColor = SoftPinkSurface,
                                labelColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(44.dp)
                        )
                        SafeZoneCategory.entries.take(3).forEach { cat ->
                            FilterChip(
                                selected = selectedFilter == cat,
                                onClick = { selectedFilter = if (selectedFilter == cat) null else cat },
                                label = { Text(cat.label.take(12), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPink,
                                    selectedLabelColor = Color.White,
                                    containerColor = SoftPinkSurface,
                                    labelColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().height(420.dp)
                    ) {
                        items(filteredZones, key = { it.id }) { zone ->
                            val distKm = SafeZonesRepository.calculateDistanceKm(userLat, userLon, zone.lat, zone.lon)
                            SafeZoneBottomSheetCard(
                                zone = zone,
                                distanceText = SafeZonesRepository.formatDistance(distKm),
                                isSelected = selectedSafeZone?.id == zone.id,
                                onSelect = { selectedSafeZone = zone },
                                onCall = { dialPhone(context, zone.emergencyContact) },
                                onDirections = { openDirections(context, zone.lat, zone.lon, zone.name) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    LocationTelemetryCard(
                        coords = currentCoords,
                        isGpsActive = isGpsActive,
                        onOpenExternalMap = {
                            openExternalMaps(context, userLat, userLon)
                        }
                    )
                }

                item {
                    // Tactical Proximity Radar Canvas Card
                    InfoCard(
                        backgroundColor = CardBackground,
                        borderColor = BorderDivider
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.offline_radar_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Rings: 500m–5km",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = PrimaryPink
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Concentric Radar Canvas with Safe-Zone Pins
                            TacticalRadarView(
                                userLat = userLat,
                                userLon = userLon,
                                safeZones = safeZones,
                                selectedZone = selectedSafeZone,
                                activeDistressAlert = latestAlert,
                                onSelectZone = { selectedSafeZone = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(290.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Radar Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendItem(PrimaryPink, "You")
                                LegendItem(SafeGreen, "Hospital")
                                LegendItem(GuardianBlue, "Police")
                                LegendItem(PrimaryPink, "Women Desk")
                                if (latestAlert != null) {
                                    LegendItem(EmergencyRed, "SOS Alert")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(250.dp))
                }
            }
        }
    }
}

@Composable
private fun LocationTelemetryCard(
    coords: com.brokencoders.narisuraksha.capture.Coordinates?,
    isGpsActive: Boolean,
    onOpenExternalMap: () -> Unit
) {
    InfoCard(
        backgroundColor = CardBackground,
        borderColor = BorderDivider
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isGpsActive) SafeGreen else VigilanceAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGpsActive) stringResource(R.string.live_gps_telemetry) else stringResource(R.string.offline_baseline_gps),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = if (isGpsActive) SafeGreen else VigilanceAmber
                    )
                }

                Button(
                    onClick = onOpenExternalMap,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Maps", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryParam("LATITUDE", coords?.let { String.format(Locale.US, "%.5f", it.lat) } ?: "—")
                TelemetryParam("LONGITUDE", coords?.let { String.format(Locale.US, "%.5f", it.lon) } ?: "—")
                TelemetryParam("ACCURACY", coords?.accuracy?.let { "±${it.toInt()} m" } ?: "Unknown")
                TelemetryParam(
                    "UPDATED",
                    coords?.timestamp?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: "No fix"
                )
            }
        }
    }
}

@Composable
private fun TelemetryParam(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            ),
            color = TextPrimary
        )
    }
}

/**
 * Concentric range rings in soft pink, labeled 500 m / 1 km / 2 km / 5 km.
 * Plots safe-zone pins with icon + name overlay badges.
 */
@Composable
private fun TacticalRadarView(
    userLat: Double,
    userLon: Double,
    safeZones: List<SafeZoneItem>,
    selectedZone: SafeZoneItem?,
    activeDistressAlert: com.brokencoders.narisuraksha.ble.ReceivedSos?,
    onSelectZone: (SafeZoneItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SoftPinkSurface)
            .border(1.5.dp, BorderDivider, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.height.coerceAtMost(size.width) / 2f) - 24.dp.toPx()

            // Concentric range rings in soft pink: 500 m, 1 km, 2 km, 5 km
            val rings = listOf(
                Pair(0.5f / 5.0f, "500 m"),
                Pair(1.0f / 5.0f, "1 km"),
                Pair(2.0f / 5.0f, "2 km"),
                Pair(5.0f / 5.0f, "5 km")
            )

            val strokeDashed = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            rings.forEach { (fraction, label) ->
                val radius = maxRadius * fraction
                drawCircle(
                    color = PrimaryPink.copy(alpha = 0.32f),
                    radius = radius,
                    center = center,
                    style = strokeDashed
                )

                // Label each ring in soft pink / Primary Pink
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#A0144A")
                    textSize = 10.sp.toPx()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    center.x,
                    center.y - radius - 2.dp.toPx(),
                    textPaint
                )
            }

            // Crosshairs
            drawLine(
                PrimaryPink.copy(alpha = 0.20f),
                Offset(center.x, center.y - maxRadius),
                Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                PrimaryPink.copy(alpha = 0.20f),
                Offset(center.x - maxRadius, center.y),
                Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Center User Dot
            drawCircle(color = PrimaryPink, radius = 9.dp.toPx(), center = center)
            drawCircle(color = Color.White, radius = 12.dp.toPx(), center = center, style = Stroke(width = 2.5.dp.toPx()))
        }

        // Overlay Interactive Safe-Zone Pins with Icon + Name
        Box(modifier = Modifier.fillMaxSize()) {
            safeZones.forEach { zone ->
                val distKm = SafeZonesRepository.calculateDistanceKm(userLat, userLon, zone.lat, zone.lon)
                val bearing = SafeZonesRepository.calculateBearing(userLat, userLon, zone.lat, zone.lon)
                val clampedDist = distKm.coerceAtMost(5.0)

                RadarPinBadge(
                    zone = zone,
                    distanceKm = clampedDist,
                    bearingDeg = bearing,
                    isSelected = selectedZone?.id == zone.id,
                    onClick = { onSelectZone(zone) }
                )
            }

            // Active distress alert pin (if present)
            activeDistressAlert?.let { alert ->
                if (alert.packet.lat != 0f && alert.packet.lon != 0f) {
                    val alertDist = SafeZonesRepository.calculateDistanceKm(userLat, userLon, alert.packet.lat.toDouble(), alert.packet.lon.toDouble())
                    val alertBearing = SafeZonesRepository.calculateBearing(userLat, userLon, alert.packet.lat.toDouble(), alert.packet.lon.toDouble())

                    AlertRadarPin(
                        distKm = alertDist.coerceAtMost(5.0),
                        bearingDeg = alertBearing.toDouble()
                    )
                }
            }
        }

        // Cardinal directions labels
        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryPink),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
        )
        Text(
            text = "S",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
        )
        Text(
            text = "E",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
        )
        Text(
            text = "W",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
        )
    }
}

/**
 * Safe-zone pin with icon + name
 */
@Composable
private fun RadarPinBadge(
    zone: SafeZoneItem,
    distanceKm: Double,
    bearingDeg: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val angleRad = Math.toRadians(bearingDeg.toDouble() - 90.0)
    // Scale within radar: 5km maps to 105dp from center
    val radiusDp = ((distanceKm / 5.0) * 105.0).coerceIn(12.0, 115.0)
    val offsetX = (radiusDp * cos(angleRad)).dp
    val offsetY = (radiusDp * sin(angleRad)).dp

    val pinColor = when (zone.category) {
        SafeZoneCategory.POLICE -> GuardianBlue
        SafeZoneCategory.HOSPITAL -> SafeGreen
        SafeZoneCategory.WOMEN_HELP_DESK -> PrimaryPink
        SafeZoneCategory.METRO_SECURITY -> VigilanceAmber
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .offset(x = offsetX, y = offsetY)
            .semantics { contentDescription = "${zone.name}, ${zone.category.label}" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (isSelected) 28.dp else 22.dp)
                    .clip(CircleShape)
                    .background(pinColor)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (zone.category) {
                        SafeZoneCategory.POLICE -> Icons.Default.LocalPolice
                        SafeZoneCategory.HOSPITAL -> Icons.Default.LocalHospital
                        SafeZoneCategory.WOMEN_HELP_DESK -> Icons.Default.Security
                        SafeZoneCategory.METRO_SECURITY -> Icons.Default.Train
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isSelected) 16.dp else 12.dp)
                )
            }

            // Name label under pin
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .border(0.5.dp, BorderDivider, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = when (zone.category) {
                        SafeZoneCategory.POLICE -> "Police"
                        SafeZoneCategory.HOSPITAL -> "Hospital"
                        SafeZoneCategory.WOMEN_HELP_DESK -> "Desk"
                        SafeZoneCategory.METRO_SECURITY -> "Metro"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = pinColor
                    )
                )
            }
        }
    }
}

@Composable
private fun AlertRadarPin(
    distKm: Double,
    bearingDeg: Double
) {
    val angleRad = Math.toRadians(bearingDeg - 90.0)
    val radiusDp = ((distKm / 5.0) * 105.0).coerceIn(12.0, 115.0)
    val offsetX = (radiusDp * cos(angleRad)).dp
    val offsetY = (radiusDp * sin(angleRad)).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .offset(x = offsetX, y = offsetY),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(EmergencyRed)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Distress SOS alert",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(EmergencyRed)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "SOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SafeZoneBottomSheetCard(
    zone: SafeZoneItem,
    distanceText: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit
) {
    InfoCard(
        backgroundColor = if (isSelected) SoftPinkSurface else CardBackground,
        borderColor = if (isSelected) PrimaryPink else BorderDivider
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when (zone.category) {
                                SafeZoneCategory.POLICE -> GuardianBlue.copy(alpha = 0.15f)
                                SafeZoneCategory.HOSPITAL -> SafeGreen.copy(alpha = 0.15f)
                                SafeZoneCategory.WOMEN_HELP_DESK -> PrimaryPink.copy(alpha = 0.15f)
                                SafeZoneCategory.METRO_SECURITY -> VigilanceAmber.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (zone.category) {
                            SafeZoneCategory.POLICE -> Icons.Default.LocalPolice
                            SafeZoneCategory.HOSPITAL -> Icons.Default.LocalHospital
                            SafeZoneCategory.WOMEN_HELP_DESK -> Icons.Default.Security
                            SafeZoneCategory.METRO_SECURITY -> Icons.Default.Train
                        },
                        contentDescription = null,
                        tint = when (zone.category) {
                            SafeZoneCategory.POLICE -> GuardianBlue
                            SafeZoneCategory.HOSPITAL -> SafeGreen
                            SafeZoneCategory.WOMEN_HELP_DESK -> PrimaryPink
                            SafeZoneCategory.METRO_SECURITY -> VigilanceAmber
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = zone.address,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    ),
                    color = SafeGreen
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Tap Call and Directions Action Buttons (56dp touch target)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call ${zone.emergencyContact}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                SecondaryButton(
                    text = "Map",
                    icon = Icons.Default.Directions,
                    onClick = onDirections,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(56.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = TextSecondary
        )
    }
}

private fun dialPhone(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun openDirections(context: Context, lat: Double, lon: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No map application installed.", Toast.LENGTH_SHORT).show()
    }
}

private fun openExternalMaps(context: Context, lat: Double, lon: Double) {
    try {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(My+Location)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No map application installed.", Toast.LENGTH_SHORT).show()
    }
}
