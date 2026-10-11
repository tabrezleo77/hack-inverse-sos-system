package com.brokencoders.narisuraksha.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brokencoders.narisuraksha.R
import com.brokencoders.narisuraksha.ui.components.InfoCard
import com.brokencoders.narisuraksha.ui.components.SecondaryButton
import com.brokencoders.narisuraksha.ui.components.StatusChip
import com.brokencoders.narisuraksha.ui.theme.BorderDivider
import com.brokencoders.narisuraksha.ui.theme.EmergencyRed
import com.brokencoders.narisuraksha.ui.theme.EmergencyRedDark
import com.brokencoders.narisuraksha.ui.theme.GuardianBlue
import com.brokencoders.narisuraksha.ui.theme.PrimaryPink
import com.brokencoders.narisuraksha.ui.theme.SafeGreen
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurface
import com.brokencoders.narisuraksha.ui.theme.TextPrimary
import com.brokencoders.narisuraksha.ui.theme.TextSecondary
import com.brokencoders.narisuraksha.ui.theme.WarningAmber
import com.brokencoders.narisuraksha.ui.viewmodels.MainViewModel

/**
 * AlertScreen - Responder & Relay View.
 *
 * Implements strict design specification:
 * 1. High urgency but readable full-screen layout.
 * 2. Distance bucket in 48sp ("Within 3 m").
 * 3. Direction arrow derived from responder coordinates & distress coordinates.
 * 4. Hop information chip using packet's hop telemetry if exposed (direct BLE link).
 * 5. 72dp green "I AM RESPONDING" button.
 * 6. Smaller "Dismiss" secondary button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(
    senderId: Short,
    lat: Float,
    lon: Float,
    rssi: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hasValidLocation = (lat != 0f || lon != 0f)

    val currentCoords by viewModel.currentCoordinates.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val isSirenOn by viewModel.isSirenOn.collectAsState()
    var isAckSent by remember { mutableStateOf(false) }

    // Bearing calculation from responder location to victim location
    val bearing = remember(currentCoords, lat, lon) {
        if (currentCoords != null && hasValidLocation) {
            calculateBearing(
                currentCoords!!.lat,
                currentCoords!!.lon,
                lat.toDouble(),
                lon.toDouble()
            )
        } else {
            null
        }
    }

    // WiFi receptions carry no RSSI: show the medium, never a dBm number.
    val viaWifiMesh = rssi == com.brokencoders.narisuraksha.ble.RSSI_WIFI_SENTINEL

    // Distance bucket text in 48sp
    val distanceBucketText = when {
        viaWifiMesh -> "via WiFi mesh"
        rssi >= -65 -> stringResource(R.string.distance_within_3m)
        rssi >= -80 -> stringResource(R.string.distance_3_to_10m)
        else -> stringResource(R.string.distance_over_10m)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.alert_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.dismiss),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High Urgency Emergency SOS Header Banner (Emergency Red)
            InfoCard(
                backgroundColor = EmergencyRed,
                borderColor = EmergencyRedDark
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.emergency_sos_active),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.sender_anonymous_id, senderId.toInt()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Proximity & Direction Hero Card (Distance bucket in 48sp + Direction Arrow)
            InfoCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.estimated_proximity),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Signal strength chip (medium chip for WiFi: no RSSI exists)
                            StatusChip(
                                label = if (viaWifiMesh) "WiFi mesh" else "$rssi dBm",
                                icon = Icons.Default.SignalCellularAlt,
                                statusColor = GuardianBlue,
                                containerColor = SoftPinkSurface
                            )

                            // Hop chip (direct BLE peer signal)
                            StatusChip(
                                label = stringResource(R.string.direct_mesh_signal),
                                icon = Icons.Default.Shield,
                                statusColor = SafeGreen,
                                containerColor = SafeGreen.copy(alpha = 0.12f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Distance bucket in 48sp with Direction Arrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = distanceBucketText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp,
                                lineHeight = 50.sp
                            ),
                            color = SafeGreen,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Direction Arrow Indicator
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SafeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bearing != null) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Direction to distress sender",
                                    tint = SafeGreen,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .rotate(bearing)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Relative proximity arrow",
                                    tint = SafeGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (bearing != null) "Compass bearing: ${bearing.toInt()}° • Calibrated BLE propagation"
                               else "Continuous radio proximity estimation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary 72dp Green "I AM RESPONDING" Button
            Button(
                onClick = {
                    val queued = viewModel.sendResponderAck(senderId)
                    if (queued) {
                        isAckSent = true
                        Toast.makeText(context, context.getString(R.string.ack_success_toast), Toast.LENGTH_LONG).show()
                    } else {
                        // ACK was dropped (cooldown, Bluetooth off, or permission
                        // missing) — do NOT claim success. Tell the user the truth.
                        Toast.makeText(context, "ACK not sent — Bluetooth off or permission missing. Please check Bluetooth and try again.", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isAckSent) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isAckSent) stringResource(R.string.ack_transmitted)
                               else stringResource(R.string.i_am_responding),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smaller "Dismiss" Button
            SecondaryButton(
                text = stringResource(R.string.dismiss),
                onClick = onBack,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // GPS Coordinates Card (Offline)
            InfoCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SoftPinkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.gps_coordinates),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (hasValidLocation) {
                        Text(
                            text = "Lat: %.5f\nLon: %.5f".format(lat, lon),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sender accuracy unknown — BLE beacons carry coordinates only; use proximity + bearing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Distress+SOS+Location)")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        context.startActivity(mapIntent)
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        Toast.makeText(context, "No offline map app installed", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open maps", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.open_maps),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            SecondaryButton(
                                text = stringResource(R.string.copy),
                                icon = Icons.Default.ContentCopy,
                                height = 56.dp,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    if (clipboard == null) {
                                        Toast.makeText(context, "Clipboard unavailable", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val clip = ClipData.newPlainText("SOS Coordinates", "$lat, $lon")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, context.getString(R.string.coordinates_copied), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_gps_indoors),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Responder Assistance Tools (Flashlight & Siren)
            InfoCard {
                Column {
                    Text(
                        text = stringResource(R.string.responder_tools),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleSiren() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSirenOn) EmergencyRed else SoftPinkSurface,
                                contentColor = if (isSirenOn) Color.White else PrimaryPink
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isSirenOn) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSirenOn) stringResource(R.string.stop_siren) else stringResource(R.string.loud_siren),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleFlashlight() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFlashlightOn) WarningAmber else SoftPinkSurface,
                                contentColor = if (isFlashlightOn) Color.White else PrimaryPink
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isFlashlightOn) Icons.Outlined.FlashlightOff else Icons.Default.FlashlightOn,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFlashlightOn) stringResource(R.string.torch_off) else stringResource(R.string.strobe_torch),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Calculates initial compass bearing in degrees (0..360) from point 1 to point 2 using the great circle formula.
 */
private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)
    val y = Math.sin(deltaLambda) * Math.cos(phi2)
    val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
    val theta = Math.atan2(y, x)
    return ((Math.toDegrees(theta) + 360.0) % 360.0).toFloat()
}
