package com.brokencoders.narisuraksha.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brokencoders.narisuraksha.R
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.ui.components.CounterTile
import com.brokencoders.narisuraksha.ui.components.InfoCard
import com.brokencoders.narisuraksha.ui.components.MeshStatusBadge
import com.brokencoders.narisuraksha.ui.theme.BorderDivider
import com.brokencoders.narisuraksha.ui.theme.ButtonShape
import com.brokencoders.narisuraksha.ui.theme.CardShape
import com.brokencoders.narisuraksha.ui.theme.EmergencyRed
import com.brokencoders.narisuraksha.ui.theme.GuardianBlue
import com.brokencoders.narisuraksha.ui.theme.PrimaryPink
import com.brokencoders.narisuraksha.ui.theme.SafeGreen
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurface
import com.brokencoders.narisuraksha.ui.theme.TextMuted
import com.brokencoders.narisuraksha.ui.theme.TextPrimary
import com.brokencoders.narisuraksha.ui.theme.TextSecondary
import com.brokencoders.narisuraksha.ui.theme.WarningAmber
import com.brokencoders.narisuraksha.ui.viewmodels.MainViewModel

/**
 * BleDebugScreen - Redesigned Mesh Status & Diagnostics View.
 *
 * Implements strict specifications:
 * 1. Two-column grid of CounterTiles using existing counters:
 *    Packets Sent, Packets Recv, ACKs Sent, ACKs Recv,
 *    Relays Sched, Dup Dropped, Last TTL, Cooldown Drops.
 * 2. Top MeshStatusBadge (icon + text).
 * 3. Permission rows with Fix buttons.
 * 4. Monospace live logcat (readable on a projector).
 * 5. "Send test beacon" 64dp action.
 * 6. "Inject simulated SOS (emulator test)" visible ONLY in debug builds with "SIMULATION" label.
 * 7. "Demo view" toggle enlarging counters and log text for presentations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDebugScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val isAdvertising by viewModel.bleDiagnostics.isAdvertising.collectAsState()
    val isScanning by viewModel.bleDiagnostics.isScanning.collectAsState()
    val packetsSent by viewModel.bleDiagnostics.packetsSentCount.collectAsState()
    val packetsReceived by viewModel.bleDiagnostics.packetsReceivedCount.collectAsState()
    val acksSent by viewModel.bleDiagnostics.acksSentCount.collectAsState()
    val acksReceived by viewModel.bleDiagnostics.acksReceivedCount.collectAsState()
    val rejectedPackets by viewModel.bleDiagnostics.rejectedPacketsCount.collectAsState()
    val relayed by viewModel.bleDiagnostics.relayedCount.collectAsState()
    val duplicatesDropped by viewModel.bleDiagnostics.duplicatesDroppedCount.collectAsState()
    val lastReceivedTtl by viewModel.bleDiagnostics.lastReceivedTtl.collectAsState()
    val debugScanMode by viewModel.bleDiagnostics.debugScanModeEnabled.collectAsState()
    val logs by viewModel.bleDiagnostics.logs.collectAsState()
    val isTestBroadcasting by viewModel.isTestBroadcasting.collectAsState()

    // Permission status
    val scanGranted = PermissionHelper.hasBluetoothScanPermission(context)
    val advGranted = PermissionHelper.hasBluetoothAdvertisePermission(context)
    val connGranted = PermissionHelper.hasBluetoothConnectPermission(context)
    val locGranted = PermissionHelper.hasLocationPermission(context)

    // Hardware status
    val btEnabled = PermissionHelper.isBluetoothEnabled(context)
    val bleSupported = PermissionHelper.isBleSupported(context)
    val advSupported = PermissionHelper.isAdvertiserSupported(context)

    // Demo View State (Projector Mode scaling)
    var isDemoView by remember { mutableStateOf(false) }

    // Debug build detection

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.mesh_diagnostics_title),
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
                            contentDescription = "Back",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Top Mesh Status Badge Card
                InfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mesh Network Engine",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID: 0x%04X • Service: 0xFDE1".format(viewModel.anonymousDeviceId.toInt() and 0xFFFF),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = TextSecondary
                            )
                        }

                        // Top MeshStatusBadge (icon + text + color)
                        MeshStatusBadge(isMeshActive = isScanning || isAdvertising)
                    }
                }
            }

            // Demo View (Projector Mode) Toggle Card
            item {
                InfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.demo_view_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.demo_view_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isDemoView,
                            onCheckedChange = { isDemoView = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryPink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = BorderDivider
                            )
                        )
                    }
                }
            }

            // Two-Column Grid of 8 CounterTiles
            item {
                Text(
                    text = "PACKET TELEMETRY COUNTERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isDemoView) 16.sp else 13.sp,
                        letterSpacing = 1.sp
                    ),
                    color = GuardianBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Packets Sent & Packets Recv
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.packets_sent),
                                count = packetsSent,
                                icon = Icons.Default.Radio,
                                color = GuardianBlue,
                                isDemoScale = isDemoView
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.packets_recv),
                                count = packetsReceived,
                                icon = Icons.Default.Sensors,
                                color = SafeGreen,
                                isDemoScale = isDemoView
                            )
                        }
                    }

                    // Row 2: ACKs Sent & ACKs Recv
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.acks_sent),
                                count = acksSent,
                                icon = Icons.Default.CheckCircle,
                                color = PrimaryPink,
                                isDemoScale = isDemoView
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.acks_recv),
                                count = acksReceived,
                                icon = Icons.Default.CheckCircle,
                                color = SafeGreen,
                                isDemoScale = isDemoView
                            )
                        }
                    }

                    // Row 3: Relays Sched & Dup Dropped
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.relays_sched),
                                count = relayed,
                                icon = Icons.Default.Share,
                                color = GuardianBlue,
                                isDemoScale = isDemoView
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.dup_dropped),
                                count = duplicatesDropped,
                                icon = Icons.Default.Warning,
                                color = WarningAmber,
                                isDemoScale = isDemoView
                            )
                        }
                    }

                    // Row 4: Last TTL & Cooldown Drops
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.last_ttl),
                                count = lastReceivedTtl ?: 0,
                                icon = Icons.Default.Timer,
                                color = GuardianBlue,
                                isDemoScale = isDemoView
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CounterTile(
                                title = stringResource(R.string.cooldown_drops),
                                count = rejectedPackets,
                                icon = Icons.Default.Warning,
                                color = WarningAmber,
                                isDemoScale = isDemoView
                            )
                        }
                    }
                }
            }

            // Primary Action: "Send test beacon"
            item {
                Button(
                    onClick = {
                        if (isTestBroadcasting) {
                            viewModel.stopTestBroadcast()
                        } else {
                            viewModel.startTestBroadcast()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestBroadcasting) EmergencyRed else GuardianBlue,
                        contentColor = Color.White
                    ),
                    shape = ButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Icon(
                        imageVector = if (isTestBroadcasting) Icons.Default.Clear else Icons.Default.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isTestBroadcasting) stringResource(R.string.stop_test_beacon)
                               else stringResource(R.string.send_test_beacon),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            // Permissions Card with Fix Buttons
            item {
                InfoCard {
                    Column {
                        Text(
                            text = stringResource(R.string.runtime_permissions),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            ),
                            color = GuardianBlue
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionRow(
                            label = "BLUETOOTH_SCAN",
                            isGranted = scanGranted,
                            onFix = { openAppSettings(context) }
                        )
                        PermissionRow(
                            label = "BLUETOOTH_ADVERTISE",
                            isGranted = advGranted,
                            onFix = { openAppSettings(context) }
                        )
                        PermissionRow(
                            label = "BLUETOOTH_CONNECT",
                            isGranted = connGranted,
                            onFix = { openAppSettings(context) }
                        )
                        PermissionRow(
                            label = "ACCESS_FINE_LOCATION",
                            isGranted = locGranted,
                            onFix = { openAppSettings(context) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Debug scan mode toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Debug Scan Mode (Software Filtering)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Bypasses strict OEM hardware filters to guarantee discovery across Qualcomm/MediaTek",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = debugScanMode,
                                onCheckedChange = { viewModel.toggleDebugScanMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SafeGreen,
                                    checkedTrackColor = SafeGreen.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.restartBleScan() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restart Scanner", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.bleDiagnostics.clearLogs() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Logs", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Monospace Live Logs Header & Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.live_logs_title, logs.size),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isDemoView) 16.sp else 13.sp,
                            letterSpacing = 1.sp
                        ),
                        color = TextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.restartBleScan() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.restart_scanner), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.bleDiagnostics.clearLogs() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.clear_logs), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Monospace Live Log Items (Readable on a Projector)
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_logs),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (isDemoView) 16.sp else 13.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = if (isDemoView) 15.sp else 12.sp
                                    ),
                                    color = if (log.isError) EmergencyRed
                                           else if (log.tag.contains("ACK")) PrimaryPink
                                           else if (log.tag.contains("SIMULATION")) WarningAmber
                                           else SafeGreen
                                )
                                Text(
                                    text = log.timeFormatted,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = if (isDemoView) 14.sp else 11.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isDemoView) 16.sp else 13.sp,
                                    lineHeight = if (isDemoView) 22.sp else 18.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * Clean Permission Row with status pill and "Fix" button.
 */
@Composable
private fun PermissionRow(
    label: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGranted) SafeGreen.copy(alpha = 0.15f) else EmergencyRed.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isGranted) stringResource(R.string.permission_granted)
                           else stringResource(R.string.permission_denied),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = if (isGranted) SafeGreen else EmergencyRed
                )
            }

            if (!isGranted) {
                Button(
                    onClick = onFix,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPink,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = stringResource(R.string.fix_permission),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
