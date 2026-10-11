package com.brokencoders.narisuraksha.capture

import kotlinx.coroutines.flow.StateFlow

/**
 * Future indoor-precision hook: WiFi-RTT (802.11mc, API 28+) and UWB
 * (Jetpack core-uwb, API 33+) ranging.
 *
 * The [LocationProvider] consults the configured [RangingProvider] before
 * falling back to GNSS, so a future implementation only needs to implement
 * this interface — no changes to SOS, SMS, or UI code. Ships as [NoOp] until
 * hardware-gated implementations land.
 */
interface RangingProvider {

    /** True when this device can currently produce a ranging fix. */
    val isAvailable: Boolean

    /**
     * Best-effort precise (1–3 m) indoor fix, or null when unavailable.
     * Must never throw: return null on any failure.
     */
    suspend fun preciseFix(): LocationFix?

    /** No indoor ranging on this build — GNSS/network chain applies. */
    object NoOp : RangingProvider {
        override val isAvailable: Boolean = false
        override suspend fun preciseFix(): LocationFix? = null
    }
}
