package com.brokencoders.narisuraksha.ble

import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation of BleTransport wrapping BleAdvertiser and BleScanner.
 *
 * Mesh relay fan-out (BLE + WiFi) is wired once in [NariSurakshaApp.onCreate]
 * via `scanner.relayHandler`, so it is never overwritten by init order.
 */
class BleTransportImpl(
    private val advertiser: BleAdvertiser,
    private val scanner: BleScanner
) : BleTransport {

    override val received: Flow<ReceivedSos> = scanner.received

    override fun startAdvertising(packet: SosPacket) {
        advertiser.startAdvertising(packet)
    }

    override fun queueRelay(packet: SosPacket) {
        advertiser.queueRelay(packet)
    }

    override fun stopAdvertising() {
        advertiser.stopAdvertising()
    }

    override fun startScanning() {
        scanner.startScanning()
    }

    override fun stopScanning() {
        scanner.stopScanning()
    }
}
