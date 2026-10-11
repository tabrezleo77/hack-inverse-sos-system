package com.brokencoders.narisuraksha.ble

import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.flow.Flow

/**
 * RSSI placeholder for WiFi receptions (WiFi has no RSSI). Display layers
 * MUST branch on [ReceivedSos.transport] (or this sentinel) and show
 * "via WiFi mesh" instead of a dBm number — never render this value.
 */
const val RSSI_WIFI_SENTINEL = Int.MIN_VALUE

data class ReceivedSos(
    val packet: SosPacket,
    val rssi: Int,
    val receivedAt: Long,
    /** Ingress medium. WiFi receptions carry no RSSI (rssi = [RSSI_WIFI_SENTINEL]). */
    val transport: MeshTransport = MeshTransport.BLE
)

interface BleTransport {
    fun startAdvertising(packet: SosPacket)
    fun queueRelay(packet: SosPacket)
    fun stopAdvertising()
    fun startScanning()
    fun stopScanning()
    val received: Flow<ReceivedSos>
}
