package com.brokencoders.narisuraksha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.brokencoders.narisuraksha.decoy.CalculatorScreen
import com.brokencoders.narisuraksha.decoy.DecoyViewModel
import com.brokencoders.narisuraksha.ui.screens.AlertScreen
import com.brokencoders.narisuraksha.ui.screens.BleDebugScreen
import com.brokencoders.narisuraksha.ui.screens.CommunityScreen
import com.brokencoders.narisuraksha.ui.screens.EmergencyContactsScreen
import com.brokencoders.narisuraksha.ui.screens.HistoryScreen
import com.brokencoders.narisuraksha.ui.screens.HomeScreen
import com.brokencoders.narisuraksha.ui.screens.MapScreen
import com.brokencoders.narisuraksha.ui.screens.OnboardingScreen
import com.brokencoders.narisuraksha.ui.screens.ProfileScreen
import com.brokencoders.narisuraksha.ui.screens.QuickRecordScreen
import com.brokencoders.narisuraksha.ui.screens.ResourcesScreen
import com.brokencoders.narisuraksha.ui.screens.SafeZoneScreen
import com.brokencoders.narisuraksha.ui.screens.SafetyAlertsScreen
import com.brokencoders.narisuraksha.ui.viewmodels.HistoryViewModel
import com.brokencoders.narisuraksha.ui.viewmodels.MainViewModel

@Composable
fun NariSurakshaNavGraph(
    navController: NavHostController,
    startDestination: String,
    mainViewModel: MainViewModel,
    historyViewModel: HistoryViewModel,
    decoyViewModel: DecoyViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = { customPin ->
                    mainViewModel.updateSecretDecoyCode(customPin)
                    mainViewModel.setOnboardingCompleted(true)
                    mainViewModel.setGuardianScanEnabled(true)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true 
                        }
                    }
                }
            )
        }

        composable(Screen.Decoy.route) {
            CalculatorScreen(
                viewModel = decoyViewModel,
                onUnlockApp = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Decoy.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSosTriggered = {
                    // Silently triggers SOS, optionally show home screen
                    navController.navigate(Screen.Home.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = mainViewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                onNavigateToSafeZones = { navController.navigate(Screen.SafeZones.route) { launchSingleTop = true } },
                onNavigateToDecoy = { navController.navigate(Screen.Decoy.route) { launchSingleTop = true } },
                onNavigateToMap = { navController.navigate(Screen.Map.route) { launchSingleTop = true } },
                onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts.route) { launchSingleTop = true } },
                onNavigateToBleDebug = { navController.navigate(Screen.BleDebug.route) { launchSingleTop = true } },
                onNavigateToAlert = { senderId, lat, lon, rssi ->
                    navController.navigate(Screen.Alert.createRoute(senderId, lat, lon, rssi)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSafetyAlerts = { navController.navigate(Screen.SafetyAlerts.route) { launchSingleTop = true } },
                onNavigateToCommunity = { navController.navigate(Screen.Community.route) { launchSingleTop = true } },
                onNavigateToResources = { navController.navigate(Screen.Resources.route) { launchSingleTop = true } },
                onNavigateToQuickRecord = { navController.navigate(Screen.QuickRecord.route) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
            )
        }

        composable(
            route = Screen.Alert.route,
            arguments = listOf(
                navArgument("senderId") { type = NavType.IntType; defaultValue = 0 },
                navArgument("lat") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("lon") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("rssi") { type = NavType.IntType; defaultValue = -70 }
            )
        ) { backStackEntry ->
            val senderId = (backStackEntry.arguments?.getInt("senderId") ?: 0).toShort()
            val lat = backStackEntry.arguments?.getFloat("lat") ?: 0f
            val lon = backStackEntry.arguments?.getFloat("lon") ?: 0f
            val rssi = backStackEntry.arguments?.getInt("rssi") ?: -70

            AlertScreen(
                senderId = senderId,
                lat = lat,
                lon = lon,
                rssi = rssi,
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SafetyAlerts.route) {
            SafetyAlertsScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                onNavigateToAlert = { senderId, lat, lon, rssi ->
                    navController.navigate(Screen.Alert.createRoute(senderId, lat, lon, rssi))
                },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                onNavigateToCommunity = { navController.navigate(Screen.Community.route) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SafeZones.route) {
            SafeZoneScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                onNavigateToCommunity = { navController.navigate(Screen.Community.route) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EmergencyContacts.route) {
            EmergencyContactsScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BleDebug.route) {
            BleDebugScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Community.route) {
            CommunityScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                onNavigateToSafetyAlerts = { navController.navigate(Screen.SafetyAlerts.route) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Resources.route) {
            ResourcesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuickRecord.route) {
            QuickRecordScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                onNavigateToSafetyAlerts = { navController.navigate(Screen.SafetyAlerts.route) { launchSingleTop = true } },
                onNavigateToCommunity = { navController.navigate(Screen.Community.route) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                onNavigateToDecoy = { navController.navigate(Screen.Decoy.route) }
            )
        }
    }
}
