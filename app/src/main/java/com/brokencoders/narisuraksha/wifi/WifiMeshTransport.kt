package com.brokencoders.narisuraksha.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.brokencoders.narisuraksha.ble.MeshCore
import com.brokencoders.narisuraksha.ble.MeshTransport
import com.brokencoders.narisuraksha.ble.PacketCodec
import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Offline WiFi mesh transport — the PRIMARY (long-range) medium.
 *
 * When the phone has WiFi on and is joined to any LAN (home router, venue
 * WiFi, or another phone's hotspot — no internet required), SOS packets are
 * flooded as UDP broadcasts (~50–100 m per hop vs ~10–30 m for BLE) using the
 * exact same [SosPacket] encoding as BLE, so range roughly triples per hop.
 *
 * When WiFi is off or unjoined, this transport idles silently and the
 * always-on BLE mesh carries everything (graceful fallback, zero crashes).
 *
 * Both media feed the single shared [MeshCore]: a broadcast heard over BLE
 * and re-heard over WiFi is one dedup-cache event, never a storm. Relay
 * fan-out transmits on BOTH media for maximum propagation.
 */
class WifiMeshTransport(
    private val context: Context,
    private val meshCore: MeshCore,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var socket: DatagramSocket? = null
    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Starts listening for WiFi mesh broadcasts. No-op (stays idle) when WiFi
     * is unavailable — BLE remains the active medium. Safe to call repeatedly.
     * Call [forceRecheck] when you know the WiFi state has changed (e.g. after
     * the system reports a network join) to trigger a fresh check without
     * restarting the app.
     */
    fun start() {
        if (listenJob?.isActive == true) return
        if (!isWifiJoined()) {
            Log.i(TAG, "WiFi mesh idle: no joined WiFi network (BLE mesh active)")
            _isActive.value = false
            return
        }
        listenJob = scope.launch {
            runListener()
        }
    }

    /**
     * Forces a fresh WiFi join check. Use this when you know the WiFi state has
     * changed (e.g. the system just connected to a new network) without requiring
     * an app restart. Safe to call from Activity.onResume() or a Connectivity
     * callback.
     */
    fun forceRecheck() {
        if (!isWifiJoined()) {
            Log.i(TAG, "WiFi recheck: network no longer joined — keeping BLE mesh active")
            _isActive.value = false
            return
        }
        // WiFi is now joined: start the listener
        listenJob = scope.launch {
            runListener()
        }
        _isActive.value = true
        Log.i(TAG, "WiFi recheck: network joined — mesh listener started")
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        _isActive.value = false
    }

    fun close() {
        stop()
        scope.cancel()
    }

    /**
     * Broadcasts one packet on the LAN (best-effort). Drops silently when WiFi
     * is unavailable or on any socket error — BLE fan-out still delivers.
     */
    fun broadcast(packet: SosPacket) {
        val payload = wrap(packet) ?: return
        externalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (!isWifiJoined()) return@withContext
                    val sock = DatagramSocket().apply { broadcast = true }
                    try {
                        for (target in broadcastAddresses()) {
                            try {
                                val dg = DatagramPacket(payload, payload.size, target, WIFI_MESH_PORT)
                                sock.send(dg)
                            } catch (e: Exception) {
                                Log.d(TAG, "WiFi send to $target failed: ${e.message}")
                            }
                        }
                    } finally {
                        try {
                            sock.close()
                        } catch (_: Exception) {
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "WiFi mesh broadcast failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun runListener() {
        var sock: DatagramSocket? = null
        try {
            sock = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = SOCKET_TIMEOUT_MS
                bind(java.net.InetSocketAddress(WIFI_MESH_PORT))
            }
            socket = sock
            _isActive.value = true
            Log.i(TAG, "WiFi mesh listening on UDP $WIFI_MESH_PORT")

            val buf = ByteArray(MAX_DATAGRAM_BYTES)
            while (scope.isActive) {
                try {
                    val dg = DatagramPacket(buf, buf.size)
                    sock.receive(dg)
                    val bytes = dg.data.copyOf(dg.length)
                    val packet = unwrap(bytes)
                    if (packet != null) {
                        // WiFi has no RSSI; distance buckets are BLE-derived, so the
                        // UI/notification layer keys off transport instead (see
                        // ReceivedSos.transport) rather than this placeholder.
                        meshCore.ingest(packet, rssi = com.brokencoders.narisuraksha.ble.RSSI_WIFI_SENTINEL, MeshTransport.WIFI)
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    // Poll timeout: re-check wifi state, exit quietly if lost.
                    if (!isWifiJoined()) {
                        Log.i(TAG, "WiFi mesh lost network, going idle")
                        break
                    }
                } catch (e: Exception) {
                    if (!scope.isActive) break
                    Log.w(TAG, "WiFi mesh receive error: ${e.message}")
                    delay(RETRY_DELAY_MS)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "WiFi mesh listener unavailable: ${e.message}")
        } finally {
            try {
                sock?.close()
            } catch (_: Exception) {
            }
            if (socket === sock) socket = null
            _isActive.value = false
        }
    }

    private fun isWifiJoined(): Boolean {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return false
            if (!wifi.isWifiEnabled) return false
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return wifi.isWifiEnabled
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun broadcastAddresses(): List<InetAddress> {
        val out = mutableListOf<InetAddress>()
        try {
            out.add(InetAddress.getByName("255.255.255.255"))
        } catch (_: Exception) {
        }
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                val ni = ifaces.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                for (addr in ni.interfaceAddresses) {
                    val bcast = addr.broadcast
                    if (bcast != null && bcast is Inet4Address) out.add(bcast)
                }
            }
        } catch (_: Exception) {
        }
        return out.distinct()
    }

    companion object {
        private const val TAG = "WifiMesh"

        /** UDP port for offline SOS flooding (unprivileged, app-specific). */
        const val WIFI_MESH_PORT = 47235

        const val ENVELOPE_MAGIC: Byte = 0x57 // 'W'
        const val ENVELOPE_VERSION: Byte = 0x01
        private const val ENVELOPE_HEADER_BYTES = 4
        private const val MAX_DATAGRAM_BYTES = 512
        private const val SOCKET_TIMEOUT_MS = 2000
        private const val RETRY_DELAY_MS = 1000L

        /**
         * Wraps an encoded [SosPacket] in the WiFi envelope:
         * `[0]='W' [1]=v1 [2..3]=payload length BE [4..]=PacketCodec bytes`.
         * Returns null when the packet cannot be encoded.
         */
        fun wrap(packet: SosPacket): ByteArray? {
            val payload = PacketCodec.encode(packet) ?: return null
            if (payload.size > MAX_DATAGRAM_BYTES - ENVELOPE_HEADER_BYTES) return null
            val buf = ByteBuffer.allocate(ENVELOPE_HEADER_BYTES + payload.size)
                .order(ByteOrder.BIG_ENDIAN)
            buf.put(ENVELOPE_MAGIC)
            buf.put(ENVELOPE_VERSION)
            buf.putShort(payload.size.toShort())
            buf.put(payload)
            return buf.array()
        }

        /**
         * Validates and decodes one WiFi datagram. Returns null for garbage,
         * wrong magic/version, truncated frames, or undecodable payloads —
         * never throws.
         */
        fun unwrap(bytes: ByteArray?): SosPacket? {
            if (bytes == null || bytes.size < ENVELOPE_HEADER_BYTES + 1) return null
            return try {
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                if (buf.get() != ENVELOPE_MAGIC) return null
                if (buf.get() != ENVELOPE_VERSION) return null
                val len = buf.short.toInt() and 0xFFFF
                if (len <= 0 || len > MAX_DATAGRAM_BYTES - ENVELOPE_HEADER_BYTES) return null
                if (bytes.size < ENVELOPE_HEADER_BYTES + len) return null
                val payload = ByteArray(len)
                buf.get(payload)
                PacketCodec.decode(payload)
            } catch (_: Exception) {
                null
            }
        }
    }
}
