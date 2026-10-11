package com.brokencoders.narisuraksha.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Warning
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brokencoders.narisuraksha.R
import com.brokencoders.narisuraksha.ble.ReceivedSos
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.ui.components.AppBottomBar
import com.brokencoders.narisuraksha.ui.components.CountdownOverlay
import com.brokencoders.narisuraksha.ui.components.HeroBannerCard
import com.brokencoders.narisuraksha.ui.components.InfoCard
import com.brokencoders.narisuraksha.ui.components.LatestUpdatesCard
import com.brokencoders.narisuraksha.ui.components.QuickActionCard
import com.brokencoders.narisuraksha.ui.components.SafetyStatusCard
import com.brokencoders.narisuraksha.ui.components.SectionHeader
import com.brokencoders.narisuraksha.ui.components.StaySafeBanner
import com.brokencoders.narisuraksha.ui.components.WomanShieldLogo
import com.brokencoders.narisuraksha.ui.theme.BorderDivider
import com.brokencoders.narisuraksha.ui.theme.CardBackground
import com.brokencoders.narisuraksha.ui.theme.CardShape
import com.brokencoders.narisuraksha.ui.theme.DarkHeading
import com.brokencoders.narisuraksha.ui.theme.EmergencyRed
import com.brokencoders.narisuraksha.ui.theme.PrimaryPink
import com.brokencoders.narisuraksha.ui.theme.SafeGreen
import com.brokencoders.narisuraksha.ui.theme.SafeGreenSurface
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurface
import com.brokencoders.narisuraksha.ui.theme.TextPrimary
import com.brokencoders.narisuraksha.ui.theme.TextSecondary
import com.brokencoders.narisuraksha.ui.theme.WarningAmber
import com.brokencoders.narisuraksha.ui.viewmodels.MainViewModel
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSafeZones: () -> Unit = {},
    onNavigateToDecoy: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToBleDebug: () -> Unit = {},
    onNavigateToAlert: (senderId: Short, lat: Float, lon: Float, rssi: Int) -> Unit = { _, _, _, _ -> },
    onNavigateToSafetyAlerts: () -> Unit = onNavigateToHistory,
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToResources: () -> Unit = onNavigateToSafeZones,
    onNavigateToQuickRecord: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val isCountingDown by viewModel.isCountingDown.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val isSosActive by viewModel.isSosActive.collectAsState()
    val currentCoords by viewModel.currentCoordinates.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val primaryContact = contacts.find { it.isPrimary } ?: contacts.firstOrNull()
    val responderAckCount by viewModel.responderAckCount.collectAsState()
    val latestAlert by viewModel.latestReceivedAlert.collectAsState()
    val isScanEnabled by viewModel.isScanEnabled.collectAsState()
    val isSirenOn by viewModel.isSirenOn.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()

    // Strict system readiness: Bluetooth + Location + runtime permissions are
    // mandatory for mesh SOS. The gate below blocks the home content until all
    // three are satisfied, with one-tap fixes for each.
    val context = LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(PermissionHelper.hasAllPermissions(context))
    }
    var hasBluetoothPerm by remember {
        mutableStateOf(PermissionHelper.hasBluetoothPermissions(context))
    }
    var isBtEnabled by remember {
        mutableStateOf(PermissionHelper.isBluetoothEnabled(context))
    }
    var isLocEnabled by remember {
        mutableStateOf(PermissionHelper.isLocationServiceEnabled(context))
    }

    fun refreshReadiness() {
        hasPermissions = PermissionHelper.hasAllPermissions(context)
        hasBluetoothPerm = PermissionHelper.hasBluetoothPermissions(context)
        isBtEnabled = PermissionHelper.isBluetoothEnabled(context)
        isLocEnabled = PermissionHelper.isLocationServiceEnabled(context)
        if (hasBluetoothPerm && isBtEnabled) {
            viewModel.ensureGuardianScanActive()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshReadiness()
    }

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshReadiness()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshReadiness()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        refreshReadiness()
    }

    val isSystemReady = hasPermissions && hasBluetoothPerm && isBtEnabled && isLocEnabled

    val lastUpdatedText = remember(currentCoords) {
        currentCoords?.timestamp?.let {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: "Today, 10:24 AM"
    }

    // Launch SOS Countdown (blocked until Bluetooth + Location are strictly on)
    val triggerSos: () -> Unit = {
        if (isSystemReady && !isSosActive && !isCountingDown) {
            viewModel.triggerSos()
        } else if (!isSystemReady) {
            refreshReadiness()
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "home" -> { /* Already on Home */ }
                        "safety_alerts", "safety" -> onNavigateToSafetyAlerts()
                        "safe_zones" -> onNavigateToSafeZones()
                        "map", "location" -> onNavigateToMap()
                        "community" -> onNavigateToCommunity()
                        "profile", "settings" -> onNavigateToProfile()
                        "emergency_contacts" -> onNavigateToContacts()
                    }
                },
                onSosClick = triggerSos
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // ==========================================
                // 1. TOP HEADER (Matching Image 1 & 2)
                // Logo, Nari-Suraksha, "Hello, Sister ♥", Notifications & Profile
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        WomanShieldLogo(size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nari-Suraksha",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp
                                    ),
                                    color = DarkHeading
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "♥",
                                    color = PrimaryPink,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Hello, Sister • Stay safe, stay strong.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextSecondary
                            )
                        }
                    }

                    // Notification & Profile Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Notification bell with unread badge (3)
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(
                                onClick = onNavigateToSafetyAlerts,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = DarkHeading,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPink)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "3",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Profile Avatar
                        IconButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SoftPinkSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = PrimaryPink,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // PERMISSIONS REQUIRED (IF BLE/LOCATION DENIED)
                // ==========================================
                if (!hasPermissions) {
                    PermissionBanner(
                        onRequestPermissions = {
                            permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
                        },
                        onOpenSettings = {
                            PermissionHelper.openAppSettings(context)
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // ==========================================
                // ACTIVE BROADCAST NOTIFICATION (IF LIVE SOS)
                // ==========================================
                if (isSosActive) {
                    ActiveSosCard(
                        responderCount = responderAckCount,
                        isSirenOn = isSirenOn,
                        isFlashlightOn = isFlashlightOn,
                        onToggleSiren = { viewModel.toggleSiren() },
                        onToggleFlashlight = { viewModel.toggleFlashlight() },
                        onStopBroadcast = { viewModel.stopSos() },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // ==========================================
                // INCOMING DISTRESS ALERT BANNER (IF RECEIVED)
                // ==========================================
                latestAlert?.let { alert ->
                    IncomingAlertBanner(
                        alert = alert,
                        onOpenAlert = {
                            onNavigateToAlert(
                                alert.packet.senderId,
                                alert.packet.lat,
                                alert.packet.lon,
                                alert.rssi
                            )
                            // Consume the sticky alert so returning from Alert
                            // doesn't re-show the banner and stack duplicates.
                            viewModel.clearReceivedAlert()
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // ==========================================
                // 2. HERO BANNER (Matching Image 1 & 2)
                // "Your Safety, Our Priority", Subtitle, SOS Pill Button, Woman Art
                // ==========================================
                HeroBannerCard(
                    onImmediateHelpClick = triggerSos,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // ==========================================
                // 3. QUICK ACTIONS SECTION (Matching Image 2)
                // 4 Cards: Emergency SOS, Share Location, Safety Mesh, Quick Record
                // ==========================================
                SectionHeader(
                    title = "Quick Actions",
                    actionText = "View All",
                    onActionClick = onNavigateToSafetyAlerts,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Emergency",
                        subtitle = "Alert nearby contacts and authorities.",
                        icon = Icons.Default.Warning,
                        badgeText = "SOS",
                        onClick = triggerSos,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = "Share Location",
                        subtitle = "Let trusted contacts know where you are.",
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Safety Mesh",
                        subtitle = "Connect with nearby women & community.",
                        icon = Icons.Default.Shield,
                        onClick = onNavigateToBleDebug,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = "Quick Record",
                        subtitle = "Capture audio/video evidence offline.",
                        icon = Icons.Default.Videocam,
                        onClick = onNavigateToQuickRecord,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // 4. SAFETY STATUS CARD (Matching Image 2)
                // "You are Safe", Location state, Share Location button
                // ==========================================
                SectionHeader(
                    title = "Safety Status",
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                SafetyStatusCard(
                    isSafe = !isSosActive,
                    lastUpdatedText = lastUpdatedText,
                    onShareLocationClick = onNavigateToMap,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // ==========================================
                // 5. LATEST UPDATES SECTION (Matching Image 2)
                // Safety Tips, Community Support, Emergency Contacts
                // ==========================================
                SectionHeader(
                    title = "Latest Updates",
                    actionText = "View All",
                    onActionClick = onNavigateToResources,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LatestUpdatesCard(
                    onTipsClick = onNavigateToResources,
                    onCommunityClick = onNavigateToCommunity,
                    onContactsClick = onNavigateToContacts,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // ==========================================
                // 6. BOTTOM STAY SAFE BANNER (Matching Image 1 & 2)
                // "You are not alone. Help is just a tap away."
                // ==========================================
                StaySafeBanner(
                    onStaySafeClick = triggerSos,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Strict readiness gate: blocks home content until Bluetooth +
            // Location + permissions are all on. Full-size overlay consumes
            // touches so SOS/mesh actions cannot fire while not ready.
            if (!isSystemReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SystemReadinessGate(
                            hasPermissions = hasPermissions,
                            hasBluetoothPerm = hasBluetoothPerm,
                            isBtEnabled = isBtEnabled,
                            isLocEnabled = isLocEnabled,
                            onGrantPermissions = {
                                permissionLauncher.launch(
                                    PermissionHelper.getRequiredPermissions().toTypedArray()
                                )
                            },
                            onEnableBluetooth = {
                                try {
                                    bluetoothEnableLauncher.launch(
                                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    )
                                } catch (_: Exception) {
                                    refreshReadiness()
                                }
                            },
                            onEnableLocation = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    )
                                } catch (_: Exception) {
                                    refreshReadiness()
                                }
                            },
                            onOpenSettings = {
                                PermissionHelper.openAppSettings(context)
                            }
                        )
                    }
                }
            }

            // Fullscreen Countdown Overlay with Haptic Ticks
            CountdownOverlay(
                isCountingDown = isCountingDown,
                remainingSeconds = countdownSeconds,
                onCancel = { viewModel.cancelCountdown() }
            )
        }
    }
}

/**
 * Strict startup gate: Bluetooth + Location + permissions must ALL be on
 * before mesh SOS can run. Each row shows live status with a one-tap fix.
 */
@Composable
private fun SystemReadinessGate(
    hasPermissions: Boolean,
    hasBluetoothPerm: Boolean,
    isBtEnabled: Boolean,
    isLocEnabled: Boolean,
    onGrantPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onEnableLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Safety System Setup",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Bluetooth and Location must stay ON for offline SOS mesh broadcasting and rescue scanning.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))

        ReadinessRow(
            ready = hasPermissions,
            icon = Icons.Default.CheckCircle,
            title = "App Permissions",
            subtitle = if (hasPermissions) "Granted" else "Bluetooth, location, audio, notifications",
            actionText = "Grant",
            onAction = onGrantPermissions
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReadinessRow(
            ready = hasBluetoothPerm && isBtEnabled,
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth",
            subtitle = when {
                !hasBluetoothPerm -> "Permission missing"
                !isBtEnabled -> "Switched OFF — tap to turn on"
                else -> "ON — mesh radio active"
            },
            actionText = "Turn On",
            onAction = {
                if (!hasBluetoothPerm) onGrantPermissions() else onEnableBluetooth()
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReadinessRow(
            ready = isLocEnabled,
            icon = Icons.Default.LocationOn,
            title = "Location Services",
            subtitle = if (isLocEnabled) "ON — GPS rescue coordinates active"
            else "Switched OFF — tap to turn on",
            actionText = "Turn On",
            onAction = onEnableLocation
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftPinkSurface,
                contentColor = PrimaryPink
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Open App Settings", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ReadinessRow(
    ready: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit
) {
    InfoCard(
        backgroundColor = if (ready) SafeGreen.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.10f),
        borderColor = if (ready) SafeGreen else WarningAmber
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (ready) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (ready) SafeGreen else WarningAmber,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (!ready) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPink,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(actionText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Calm active broadcast indicator shown when SOS is live
 */
@Composable
private fun ActiveSosCard(
    responderCount: Int,
    isSirenOn: Boolean,
    isFlashlightOn: Boolean,
    onToggleSiren: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onStopBroadcast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, CardShape, spotColor = EmergencyRed.copy(alpha = 0.25f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmergencyRed)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmergencyRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISTRESS SIGNAL BROADCASTING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = EmergencyRed
                    )
                }

                Text(
                    text = "0xFDE1 • 60s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Help is being alerted. Stay calm.",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = DarkHeading
            )
            Text(
                text = "Nearby phones are passing your alert along over offline BLE mesh.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Responder Counter Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (responderCount > 0) SafeGreenSurface else SoftPinkSurface)
                    .border(1.dp, if (responderCount > 0) SafeGreen else BorderDivider, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (responderCount > 0) Icons.Default.CheckCircle else Icons.Default.Radio,
                    contentDescription = null,
                    tint = if (responderCount > 0) SafeGreen else PrimaryPink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (responderCount > 0) "$responderCount Nearby Responder(s) Acknowledged!" else "Listening for Nearby Responders...",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = if (responderCount > 0) SafeGreen else DarkHeading
                    )
                    Text(
                        text = if (responderCount > 0) "A bystander phone has received your distress packet." else "Transmitting 17-byte BLE beacon across nearby mesh.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Deterrent tools: loud siren + strobe torch while broadcasting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleSiren,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSirenOn) EmergencyRed else SoftPinkSurface,
                        contentColor = if (isSirenOn) Color.White else PrimaryPink
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isSirenOn) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSirenOn) stringResource(R.string.stop_siren) else stringResource(R.string.loud_siren),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onToggleFlashlight,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFlashlightOn) WarningAmber else SoftPinkSurface,
                        contentColor = if (isFlashlightOn) Color.White else PrimaryPink
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Outlined.FlashlightOff else Icons.Default.FlashlightOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFlashlightOn) stringResource(R.string.torch_off) else stringResource(R.string.strobe_torch),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStopBroadcast,
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "I'm Safe (Stop Broadcast)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Prominent banner displayed when permissions required for offline mesh & SOS are missing.
 */
@Composable
private fun PermissionBanner(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier,
        backgroundColor = WarningAmber.copy(alpha = 0.12f),
        borderColor = WarningAmber
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WarningAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bluetooth and Location are required for offline SOS broadcasting and beacon scanning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPink,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Grant Permissions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftPinkSurface,
                        contentColor = PrimaryPink
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("App Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * High-priority alert banner for incoming nearby distress beacons
 */
@Composable
private fun IncomingAlertBanner(
    alert: ReceivedSos,
    onOpenAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAlert),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = SoftPinkSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryPink)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmergencyRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "🚨 Nearby Distress Beacon Detected",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = EmergencyRed
                    )
                    Text(
                        text = "Sender #${alert.packet.senderId} • Tap to view distance & assistance tools",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Alert",
                tint = PrimaryPink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
