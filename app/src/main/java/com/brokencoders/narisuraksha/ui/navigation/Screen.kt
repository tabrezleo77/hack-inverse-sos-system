package com.brokencoders.narisuraksha.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Decoy : Screen("decoy")
    data object Home : Screen("home")
    data object Alert : Screen("alert?senderId={senderId}&lat={lat}&lon={lon}&rssi={rssi}") {
        fun createRoute(senderId: Short, lat: Float, lon: Float, rssi: Int): String {
            // NavType.FloatType crashes on NaN/Infinity — sanitize at the single
            // choke point so every caller is safe.
            val safeLat = if (lat.isFinite()) lat else 0f
            val safeLon = if (lon.isFinite()) lon else 0f
            val safeSender = senderId.toInt()
            return "alert?senderId=$safeSender&lat=$safeLat&lon=$safeLon&rssi=$rssi"
        }
    }
    data object SafetyAlerts : Screen("safety_alerts")
    data object History : Screen("history")
    data object SafeZones : Screen("safe_zones")
    data object Settings : Screen("settings")
    data object Map : Screen("map")
    data object EmergencyContacts : Screen("emergency_contacts")
    data object BleDebug : Screen("ble_debug")
    data object Community : Screen("community")
    data object Resources : Screen("resources")
    data object QuickRecord : Screen("quick_record")
    data object Profile : Screen("profile")
}
