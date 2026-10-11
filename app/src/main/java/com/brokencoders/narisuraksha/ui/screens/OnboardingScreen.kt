package com.brokencoders.narisuraksha.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brokencoders.narisuraksha.R
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.ui.components.InfoCard
import com.brokencoders.narisuraksha.ui.components.PrimaryButton
import com.brokencoders.narisuraksha.ui.theme.BorderDivider
import com.brokencoders.narisuraksha.ui.theme.EmergencyRed
import com.brokencoders.narisuraksha.ui.theme.PrimaryPink
import com.brokencoders.narisuraksha.ui.theme.SafeGreen
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurface
import com.brokencoders.narisuraksha.ui.theme.TextPrimary
import com.brokencoders.narisuraksha.ui.theme.TextSecondary

/**
 * OnboardingScreen - 3 short steps + PIN step.
 *
 * Implements strict design specifications:
 * 1. Flat solid-shape illustration per step.
 * 2. Big headline and one plain-language sentence:
 *    - "Bluetooth lets nearby phones hear your alert."
 * 3. Progress dots.
 * 4. Battery-optimization step with copy:
 *    "Some phones stop the app in the background. Tap to keep Silent SOS running."
 * 5. Large number pad for the 4-digit PIN.
 * 6. Big Continue button (64dp).
 */
@SuppressLint("BatteryLife")
@Composable
fun OnboardingScreen(
    onComplete: (customPin: String) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var userPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var permissionRequestAttempted by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    var isBatteryOptIgnored by remember {
        mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var hasPermissions by remember {
        mutableStateOf(PermissionHelper.hasAllPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = PermissionHelper.hasAllPermissions(context)
        if (!hasPermissions) {
            permissionRequestAttempted = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermissions = PermissionHelper.hasAllPermissions(context)
                isBatteryOptIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Step Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isActive = index == currentStep
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isActive) 28.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isActive) PrimaryPink else BorderDivider)
                    )
                }
            }

            // Middle Section: Step Content with Animated Transition
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStepTransition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> OnboardingMeshStep()
                    1 -> OnboardingBatteryStep(
                        isIgnored = isBatteryOptIgnored,
                        onRequestBattery = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(fallback)
                                    } catch (_: Exception) {
                                        // OEM without battery-opt settings (Xiaomi/Huawei) — re-check on resume.
                                    }
                                }
                            }
                            // Don't assume success; ON_RESUME re-check corrects the flag.
                            isBatteryOptIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
                        }
                    )
                    2 -> OnboardingPermissionsStep(
                        hasPermissions = hasPermissions,
                        permissionAttempted = permissionRequestAttempted,
                        onRequestPermissions = {
                            permissionRequestAttempted = true
                            permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
                        },
                        onOpenSettings = {
                            PermissionHelper.openAppSettings(context)
                        }
                    )
                    3 -> OnboardingPinStep(
                        pin = userPin,
                        error = pinError,
                        onDigitPress = { digit ->
                            if (userPin.length < 4) {
                                userPin += digit
                                pinError = null
                            }
                        },
                        onBackspace = {
                            if (userPin.isNotEmpty()) {
                                userPin = userPin.dropLast(1)
                                pinError = null
                            }
                        }
                    )
                }
            }

            // Bottom Section: Big Continue Button (64dp tall, full width)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryButton(
                    text = when {
                        currentStep == 3 -> "Start Protection"
                        currentStep == 2 && !hasPermissions -> "Grant Permissions"
                        else -> "Continue"
                    },
                    onClick = {
                        if (currentStep == 2 && !hasPermissions) {
                            if (!permissionRequestAttempted) {
                                permissionRequestAttempted = true
                                permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
                            } else {
                                showPermissionDeniedDialog = true
                            }
                        } else if (currentStep < 3) {
                            currentStep += 1
                        } else {
                            if (userPin.length == 4) {
                                onComplete(userPin)
                            } else {
                                pinError = "Please enter all 4 digits"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (showPermissionDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDeniedDialog = false },
                title = { Text("Permissions Required", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Bluetooth and Location permissions are required for offline SOS broadcasting and beacon scanning. Please enable them in App Settings to ensure your safety network functions.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDeniedDialog = false
                            PermissionHelper.openAppSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                    ) {
                        Text("Open App Settings")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showPermissionDeniedDialog = false
                            currentStep += 1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Skip Anyway")
                    }
                }
            )
        }
    }
}

/**
 * Step 0: Bluetooth Mesh
 * Plain-language: "Bluetooth lets nearby phones hear your alert."
 */
@Composable
private fun OnboardingMeshStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Flat solid-shape illustration
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(SoftPinkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Offline Safety Network",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Bluetooth lets nearby phones hear your alert even when cellular data and internet are dead.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Step 1: Battery Optimization Step
 * Spec Copy: "Some phones stop the app in the background. Tap to keep Silent SOS running."
 */
@Composable
private fun OnboardingBatteryStep(
    isIgnored: Boolean,
    onRequestBattery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Flat solid-shape illustration
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(if (isIgnored) SafeGreen.copy(alpha = 0.15f) else SoftPinkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIgnored) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = if (isIgnored) SafeGreen else PrimaryPink,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Always-On Guardian",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Required specification copy
        Text(
            text = "Some phones stop the app in the background. Tap to keep Silent SOS running.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestBattery,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isIgnored) SafeGreen else PrimaryPink,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (isIgnored) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isIgnored) "Background Protection Active" else "Keep Silent SOS Running",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Step 2: Essential Permissions
 */
@Composable
private fun OnboardingPermissionsStep(
    hasPermissions: Boolean,
    permissionAttempted: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Flat solid-shape illustration
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(if (hasPermissions) SafeGreen.copy(alpha = 0.15f) else SoftPinkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasPermissions) Icons.Default.CheckCircle else Icons.Default.MyLocation,
                contentDescription = null,
                tint = if (hasPermissions) SafeGreen else PrimaryPink,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Instant Telemetry",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Location and Bluetooth permissions allow your exact GPS coordinates to broadcast directly to nearby helpers without internet.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasPermissions) SafeGreen else PrimaryPink,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (hasPermissions) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasPermissions) "Permissions Granted" else "Grant Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (!hasPermissions && permissionAttempted) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftPinkSurface,
                    contentColor = PrimaryPink
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
            ) {
                Text(
                    text = "Open App Settings",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Step 3: Secret 4-Digit PIN Setup
 * With large custom number pad.
 */
@Composable
private fun OnboardingPinStep(
    pin: String,
    error: String?,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Secret Decoy PIN",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter 4 digits to trigger SOS or unlock the disguised calculator.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4 PIN indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) PrimaryPink else SoftPinkSurface)
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = EmergencyRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Number Pad (64dp tall buttons, big type, stress-proof)
        val keypadRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (key == "⌫") {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SoftPinkSurface)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = PrimaryPink,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SoftPinkSurface)
                                    .clickable { onDigitPress(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
