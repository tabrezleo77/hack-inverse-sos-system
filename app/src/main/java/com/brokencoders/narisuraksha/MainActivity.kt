package com.brokencoders.narisuraksha

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brokencoders.narisuraksha.decoy.DecoyViewModel
import com.brokencoders.narisuraksha.ui.navigation.NariSurakshaNavGraph
import com.brokencoders.narisuraksha.ui.navigation.Screen
import com.brokencoders.narisuraksha.ui.theme.NariSurakshaTheme
import com.brokencoders.narisuraksha.ui.viewmodels.HistoryViewModel
import com.brokencoders.narisuraksha.ui.viewmodels.MainViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.service.ScanForegroundService

class MainActivity : ComponentActivity() {

    // Tracks the latest intent (updated in onNewIntent) so tapping an SOS
    // notification while the app is already open still navigates to Alert.
    // LaunchedEffect(intent) alone never retriggers because Intent is not State.
    private var pendingAlertIntent by mutableStateOf<Intent?>(null)

    private val app by lazy { application as NariSurakshaApp }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(
            context = applicationContext,
            sosManager = app.sosManager,
            bleTransport = app.bleTransport,
            shakeDetector = app.shakeDetector,
            preferencesRepository = app.preferencesRepository,
            deviceIdProvider = app.deviceIdProvider,
            emergencyContactDao = app.emergencyContactDao,
            locationProvider = app.locationProvider,
            bleAdvertiser = app.bleAdvertiser,
            bleScanner = app.bleScanner,
            wifiMesh = app.wifiMesh
        )
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModel.provideFactory(
            sosDao = app.database.sosDao(),
            audioDir = java.io.File(applicationContext.filesDir, "sos_recordings")
        )
    }

    private val decoyViewModel: DecoyViewModel by viewModels {
        DecoyViewModel.provideFactory(
            preferencesRepository = app.preferencesRepository,
            sosManager = app.sosManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingAlertIntent = intent

        setContent {
            NariSurakshaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
                    val isDecoyEnabled by mainViewModel.isDecoyEnabled.collectAsState()

                    // Discretion and privacy protection (FLAG_SECURE):
                    // Blanks out the app preview thumbnail in Android Recents screen
                    // whenever inside real emergency screens (Home, Alert, History, SafeZones)
                    LaunchedEffect(currentRoute) {
                        if (currentRoute == Screen.Decoy.route) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }

                    // Determine start destination
                    val startDestination = when {
                        !isOnboardingCompleted -> Screen.Onboarding.route
                        isDecoyEnabled -> Screen.Decoy.route
                        else -> Screen.Home.route
                    }

                    NariSurakshaNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        mainViewModel = mainViewModel,
                        historyViewModel = historyViewModel,
                        decoyViewModel = decoyViewModel
                    )

                    // Handle intent when opened from Alert Notification
                    LaunchedEffect(pendingAlertIntent) {
                        handleNotificationIntent(pendingAlertIntent, navController)
                        // Consume so recomposition doesn't re-navigate and stack duplicates.
                        if (pendingAlertIntent?.getBooleanExtra("OPEN_ALERT_SCREEN", false) == true) {
                            pendingAlertIntent = null
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAlertIntent = intent
    }

    private fun handleNotificationIntent(intent: Intent?, navController: androidx.navigation.NavHostController) {
        if (intent?.getBooleanExtra("OPEN_ALERT_SCREEN", false) == true) {
            val senderId = intent.getShortExtra("SENDER_ID", 0)
            val lat = intent.getFloatExtra("LAT", 0f).let { if (it.isFinite()) it else 0f }
            val lon = intent.getFloatExtra("LON", 0f).let { if (it.isFinite()) it else 0f }
            val rssi = intent.getIntExtra("RSSI", -70)

            try {
                navController.navigate(Screen.Alert.createRoute(senderId, lat, lon, rssi)) {
                    launchSingleTop = true
                }
            } catch (_: Exception) {
                // Navigation race (e.g. graph not ready) — drop the deep-link.
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        if(PermissionHelper.hasBluetoothPermissions(this))
        {
            lifecycleScope.launch{
                val enabled = app.preferencesRepository.isGuardianScanEnabled.first()
                // prefs.first() can complete after onPause on slow devices, turning
                // this into a background FGS start (ForegroundServiceStartNotAllowed
                // on API 31+). Re-check foreground state before starting.
                if(enabled && !isFinishing && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
                {
                    ScanForegroundService.start(this@MainActivity)
                }
            }
        }
    }
}
