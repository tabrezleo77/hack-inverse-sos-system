# 🛡️ Nari-Suraksha (Silent SOS)
> **"Network gayab, fir bhi help alive."**  
> Offline-First Women's Safety & Multi-Hop BLE Mesh Distress System over Bluetooth Low Energy (BLE) with Decoy Mode, 2-Way Responder ACK, Emergency Contacts & Tactical Radar.

[![Live Demo Web App](https://img.shields.io/badge/Live%20Demo-Vercel%20App-ff2d78?style=for-the-badge&logo=vercel&logoColor=white)](https://hackinverse-women-safety-sos-fronte.vercel.app/)
[![Download Android APK](https://img.shields.io/badge/Download-Android%20APK%20(18.3MB)-e91e63?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Hasnainyt/hackinverse-women-safety-sos/raw/main/app-debug.apk)
[![GitHub Repo](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Hasnainyt/hackinverse-women-safety-sos)

---

## 🌐 Live Web Portal & Simulator

👉 **[Launch Nari-Suraksha Live Web Hub (Vercel)](https://hackinverse-women-safety-sos-fronte.vercel.app/)**  
*(Experience the interactive 3-Phone BLE mesh simulation — Phone A (victim) → Phone B (relay) → Phone C (responder), plus the ACK return path — 3-second countdown, Decoy Calculator PIN trigger, and Emergency Contacts action directly in your browser. Note: This web portal is an illustrative demonstration — web browsers do not broadcast native BLE packets. Download the Android APK for real hardware radio testing).*

---

## 📥 Direct APK Download & Quick Install

You can download and install the ready-to-run Android APK directly onto your phone:

👉 **[📥 Download Nari-Suraksha Debug APK (`app-debug.apk`)](https://github.com/Hasnainyt/hackinverse-women-safety-sos/raw/main/app-debug.apk)**

### 📲 How to Install:
1. **Download `app-debug.apk`** on your Android phone using the direct download link above (or transfer via WhatsApp, Telegram, or USB).
2. Tap the downloaded APK on your phone and select **Install**.
3. *If prompted by Google Play Protect:* Because this APK is distributed directly outside the Google Play Store, Play Protect may display an *"Unrecognized app"* warning. Tap **"More details"** and then **"Install anyway"**.
4. Open the app named **"Calculator"** (the decoy launcher icon for Nari-Suraksha), complete the 1-minute permission setup, and choose your custom 4-digit secret PIN.

---

## 🌟 Overview
**Nari-Suraksha** is an offline-first Android safety application designed for situations where **cellular networks, internet connectivity, or cloud servers are unavailable, jammed, or disabled**. 

Using autonomous multi-hop **Bluetooth Low Energy (BLE) mesh flooding**, an emergency signal hops from phone to phone — **Phone A (victim) → Phone B → Phone C → Phone D → …** — directly to nearby smartphones running the app, without relying on telecom towers, SIM data, or Wi-Fi. Every phone in range both **receives and re-broadcasts** the distress packet (with TTL decrement, deduplication, and battery-safe rate limits), so the alert travels far beyond a single Bluetooth range (~10–30 m per hop, up to 4 hops by default).

---

## 🚀 Key Features

- 📴 **100% Offline Multi-Hop BLE Mesh Network**: Autonomous BLE mesh flooding using 16-bit Service UUID (`0xFDE1`) fitting within standard legacy 31-byte BLE packets (17-byte wire payload: `magic`, `version`, `senderId`, `timestamp/targetId`, `lat`, `lon`, `flags+TTL`). Every device is simultaneously a **receiver and a relay**: Phone A broadcasts → Phone B re-broadcasts (TTL-1) → Phone C re-broadcasts (TTL-1) → Phone D, extending range hop-by-hop with no pairing and no internet.
- 🕸️ **Flooding Mesh Relay Engine** (`BleScanner` → `RelayPolicy` → `BleAdvertiser`):
  - **TTL-bounded hops**: each SOS starts with `TTL = 4` (protocol v2, upper 4 bits of the flags byte); every relay decrements TTL, stopping at 0.
  - **Deduplication**: `RelayDedupCache` (256 entries, 60 s expiry) drops already-seen packets so loops and re-floods never multiply.
  - **Battery-safe budgets**: max 12 relays/minute per device + 12/minute mesh budget, with 200–1500 ms randomized jitter to avoid air collisions.
  - **Priority queue**: own SOS (rank 1) preempts own ACK (rank 2) and relay traffic (rank 3); the single hardware advertiser slot is never stolen by a relay.
- 📶 **WiFi-First, Bluetooth-Always Dual Mesh**: WiFi is the **primary** medium — when joined to any LAN (home router, venue WiFi, phone hotspot; no internet needed), SOS floods as UDP broadcasts (port `47235`, same 17-byte packet encoding) at ~50–100 m per hop. Bluetooth LE is the **always-on fallback** (~10–30 m/hop) that carries everything when WiFi is off or unjoined. Both media feed **one shared `MeshCore`** (single dedup cache, TTL policy, budgets), so a broadcast heard on both is one event — never a storm — and every relay fans out on **both** media. WiFi needs only normal install-time permissions (`INTERNET`, `ACCESS_WIFI_STATE`): no extra runtime prompt.

- ⚡ **Multi-Trigger SOS with 3-Second Confirmation**:
  - **1-Tap Pulsating Hero SOS Button** with tactile ring feedback.
  - **3-Shake Accelerometer Sensor**: Detects 3 rapid shakes (> 2.7g) within 2 seconds with a 3-second cooldown and 220ms debounce to prevent false alarms from table bumps.
  - **3-Second Cancel Window**: Fast countdown overlay with audio/vibration cues allowing the user to abort accidental triggers.
- 🧮 **Decoy Mode (Calculator Disguise) & Privacy**:
  - Fully functional arithmetic calculator (`+`, `-`, `×`, `÷`, `%`) named **"Calculator"** with a neutral calculator icon.
  - Custom 4-digit secret PIN configured during onboarding.
  - Entering the secret PIN followed by `=` covertly triggers the 3-second SOS countdown.
  - Long-pressing `=` unlocks the full guardian interface.
  - **FLAG_SECURE Protection**: Prevents app screenshots and blanks out Android Recents thumbnail previews when viewing emergency screens.
- 👥 **2-Way Responder Acknowledgment (ACK) & Live Counter**:
  - Responders tap **"I AM RESPONDING (SEND ACK)"** to transmit a targeted BLE confirmation beacon back to the victim.
  - Responders are uniquely tracked and deduplicated: tapping twice increments the counter only once.
  - ACKs match only the victim who broadcast distress; other victims ignore ACKs addressed elsewhere.
- 📞 **Emergency Contacts & SMS Integration**:
  - Local Room database with `MIGRATION_1_2` (preserves all data without destructive fallback).
  - Add, edit, delete, and designate Primary emergency contact.
  - Direct one-tap phone call via Android dialer (`ACTION_DIAL`).
  - One-tap Emergency SMS pre-filled with live GPS coordinates, Google Maps link, and timestamp (BLE SOS continues working even when cellular network is unavailable).
- 🗺️ **Tactical Offline Proximity Radar & Safe Zones**:
  - 100% offline Canvas radar with concentric range rings (500m, 1km, 2km, 5km) and cardinal compass orientation.
  - Real-time GPS telemetry card (Latitude, Longitude, Accuracy in meters, Timestamp).
  - Offline pre-cached directory of 24x7 Women Help Desks, Police Stations, Hospitals, and Transit Security booths with local Haversine distance and bearing calculations.
- 🎙️ **Local Audio Evidence Recording**: Automatically records microphone audio to app-private storage upon SOS confirmation (configured with `FOREGROUND_SERVICE_MICROPHONE` for Android 14/15 compliance). Never blocks SOS if microphone access fails.
- 🔍 **BLE Diagnostics & Test Mode Screen**:
  - Accessible via Settings -> **BLE DIAGNOSTICS & TEST MODE**.
  - Live hardware status (Bluetooth, BLE, Advertiser, Scanner).
  - Runtime permissions matrix (`BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`).
  - Real-time telemetry counters (Packets Sent/Received, ACKs Sent/Received, Rejected Packets).
  - Live structured event log stream (`NARI_BLE_*`).
  - Debug scan mode toggle (software-filtered scan bypassing strict OEM chip filters).
  - Non-emergency **"START TEST BROADCAST"** button transmitting `FLAG_TEST` beacon.
- 🛡️ **Always-On Background Guardian**: Persistent background service ("Calculator Service") with low-visibility notifications that continues listening for distress beacons even when the screen is locked.
- 🛑 **Dual-Tier Rate Limiting & Abuse Prevention**: 10-second per-sender spam rejection combined with a global cap (max 10 alerts/minute across all senders) to mitigate spoofed ID-rotation floods.

---

## 🏗️ Architecture

The project is built on modern Android standards:
- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose with Material 3 (High-Contrast Dark Theme, `EmergencyRed`, `GuardianBlue`, `SafeGreen`, `VigilanceAmber`)
- **Architecture Pattern**: Reactive MVVM / MVI with Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Local Persistence**: Room 2.6 (Database `version = 2` with explicit migration `MIGRATION_1_2`) & Jetpack DataStore Preferences
- **Hardware Integration**: Android Bluetooth Low Energy APIs (`BluetoothLeAdvertiser`, `BluetoothLeScanner`), Android Sensor Framework (`Sensor.TYPE_ACCELEROMETER`), Google Play Services Location (`FusedLocationProviderClient`)
- **Mesh Relay Pipeline**: `BleScanner.processScanResult` → `RelayPolicy.evaluate` (ignore-own / dedup / TEST / ACK-routing / SOS) → `scheduleRelay` (TTL-1, jitter) → `BleAdvertiser.queueRelay` (priority queue, battery cap) → hardware broadcast; `RelayDedupCache` + `RelayBudgetTracker` + `PacketCodec` (TTL in flags byte, protocol v2)
- **Target SDK**: Android 15 (API 35) | **Min SDK**: Android 8.0 (API 26)

---

## 📡 BLE Advertisement Packet Specification

To ensure universal compatibility across Android chipsets (Samsung, Xiaomi, Vivo/Oppo, Pixel, OnePlus), Nari-Suraksha packages all distress telemetry strictly within the legacy 31-byte Bluetooth advertising packet:

```
[ AD Record 1: 16-bit Complete Service UUID List ] (4 Bytes)
  ├── Length: 0x03 (3 bytes)
  ├── AD Type: 0x03 (16-bit Complete Service UUID List)
  └── Service UUID: 0xFDE1 (2 bytes)

[ AD Record 2: 16-bit Service Data ] (21 Bytes)
  ├── Length: 0x14 (20 bytes following: AD Type 1B + UUID 2B + SOS Wire Payload 17B)
  ├── AD Type: 0x16 (Service Data - 16-bit UUID)
  ├── UUID: 0xFDE1 (2 bytes)
  └── SOS Wire Payload: 17 Bytes
        ├── Offset 0:     Magic Byte: 0x53 ('S') (1 byte)
        ├── Offset 1:     Protocol Version: 0x02 (mesh TTL; 0x01 = legacy, TTL treated as 0) (1 byte)
        ├── Offset 2..3:  Sender ID: Short (2 bytes, Big-Endian)
        ├── Offset 4..7:  Timestamp / Target Sender ID: Int (4 bytes, Big-Endian)
        ├── Offset 8..11: Latitude: Float IEEE 754 (4 bytes)
        ├── Offset 12..15: Longitude: Float IEEE 754 (4 bytes)
        └── Offset 16:    Flags + TTL: (1 byte)
                          ├── Bits 7..4: TTL / hop budget, 0–15 (default 4; decremented per relay)
                          ├── Bit 0: SOS Active (0x01)
                          ├── Bit 1: Location Unavailable (0x02)
                          ├── Bit 2: ACK Beacon (0x04)
                          └── Bit 3: Test Broadcast (0x08)

Total AD Structure: 4 (Record 1) + 21 (Record 2) = 25 Bytes <= 31 Bytes (Universal compatibility)
```

> **Legacy fallback**: 15-byte unversioned packets (no magic/version, TTL = 0, never relayed) still decode for backwards compatibility.

### 🕸️ Mesh SOS Relay Flow (Phone A → B → C → D … via Bluetooth)

No pairing. No internet. Every phone running the app is a relay node:

1. **Phone A (Victim)** triggers SOS and broadcasts with `senderId = 0x1A2B`, `flags = SOS`, `TTL = 4`.
2. **Phone B** (within ~10–30 m BLE range) scans and discovers Phone A's beacon:
   - Displays the Incoming Alert screen locally (`ALERT_AND_RELAY`).
   - Waits a random 200–1500 ms (collision avoidance), decrements TTL to 3, and **re-broadcasts** the same distress packet.
3. **Phone C** (out of Phone A's range, but in Phone B's range) receives Phone B's relay (`TTL = 3`), alerts locally, re-broadcasts with `TTL = 2`.
4. **Phone D** receives from Phone C (`TTL = 2`), alerts locally, re-broadcasts with `TTL = 1` — and so on until `TTL = 0`, which is delivered locally but never forwarded again.
5. **Loop protection**: if Phone B hears its own relay echoed back (same `senderId + timestamp + flags`), `RelayDedupCache` drops it as a duplicate; per-sender (10 s) and global (10/min) rate limits stop spoof floods; TEST beacons (`FLAG_TEST`) alert locally but are never relayed.

### 2-Way Handshake ACK Flow (Responder → Victim, also via mesh)
1. **Phone D (Responder)** discovers the relayed SOS and displays the Incoming Alert screen.
2. Responder taps **"I AM RESPONDING"**.
3. **Phone D** broadcasts an ACK beacon with:
   - `senderId = 0x5C6D` (Responder's device ID)
   - `timestamp = 0x1A2B` (Carries Phone A's sender ID as `targetSenderId`)
   - `flags = ACK`, `TTL = 4`
4. **Intermediate phones (C, B)** see an ACK addressed to someone else (`RELAY_ONLY`): no alert, just TTL-decrement + re-broadcast toward Phone A.
> **No hardcoded IDs anywhere**: the ACK target is always the victim's live sender ID from the received packet, and responder IDs are the responders' live device IDs.

5. **Phone A** receives the ACK (over BLE *or* WiFi — the responder transmits 3 bursts on **both** media, and intermediate phones relay foreign ACKs on both):
   - Validates `targetSenderId == myDeviceId`.
   - Deduplicates responder: adds `0x5C6D` to unique `Set<Short>`.
   - Increments responder count (`0 -> 1`).
   - Another device's ACK or duplicate packet from the same responder is deduplicated without re-incrementing.

---

### 📶 WiFi Mesh Wire Format (UDP Broadcast)

```
[ WiFi UDP datagram -> port 47235 ] (21 Bytes)
  ├── Offset 0:     Envelope Magic: 0x57 ('W') (1 byte)
  ├── Offset 1:     Envelope Version: 0x01 (1 byte)
  ├── Offset 2..3:  Payload Length: UShort (2 bytes, Big-Endian)
  └── Offset 4..20: SOS Wire Payload: 17 Bytes (identical to the BLE packet above, TTL included)
```

LAN noise that fails magic/version/length checks is dropped before decode, so busy networks can never inject phantom SOS alerts.

---

## 📳 Shake Detection Engine Specification

The shake detector uses a dedicated, testable algorithm engine (`ShakeDetectionEngine`):
- **Acceleration Threshold**: `2.7g` (~26.48 m/s²)
- **Required Spikes**: `3 spikes`
- **Sliding Window**: `2000 ms` (2 seconds)
- **Minimum Spike Interval (Debouncing)**: `220 ms`
  - *Why this is critical*: When a phone drops or is placed on a table, the accelerometer produces rapid oscillation samples within 10–50ms. Debouncing with a 220ms minimum interval guarantees that each spike corresponds to a human shaking stroke, eliminating false positives from normal handling, table placement, walking, or screen rotation.
- **Post-Trigger Cooldown**: `3000 ms` (3 seconds) between alerts.
- **Diagnostics**: Real-time G-force, peak G-force, spike count, last spike timestamp, and cooldown status viewable live in the Diagnostics screen.

---

## 📍 Precise Real-Time Location Tracking

- **Continuous GNSS tracking**: `PRIORITY_HIGH_ACCURACY` updates (5 s interval / 2 s fastest / 5 m displacement) keep a fresh `LocationFix` at all times, so SOS sends the latest position with zero acquisition delay.
- **Honest precision model**: every fix carries `accuracyM` (±m radius), `timestamp`, and `provider`. Quality tiers — `HIGH` (≤15 m, ≤15 s), `MEDIUM` (≤50 m, ≤60 s), `LOW`, `STALE` (>120 s) — drive UI badges. Coordinates are never shown without their accuracy; unknown precision renders as "Unknown", never a fabricated number.
- **Fallback chain** (each stage time-boxed, never blocks SOS): fresh tracked fix → indoor ranging (future RTT/UWB hook) → high-accuracy GNSS one-shot → balanced/network one-shot → last-known → `null` (`FLAG_LOCATION_UNAVAILABLE`).
- **SOS integration**: on activation, the latest fix + accuracy + age + Google Maps link (`https://maps.google.com/?q=lat,lon`) go to emergency contacts via SMS; while the alert broadcasts, the victim's beacon **re-broadcasts live** on movement (≥25 m) or every 60 s so responders converge on the current position.
- **Graceful failures**: permission checks before every request, location-disabled detection, per-stage timeouts, `SecurityException` guards for mid-flight revocation.
- **Future-proof**: `RangingProvider` interface (WiFi-RTT 802.11mc / UWB core-uwb) slots in with zero changes to SOS, SMS, or UI code. The web hub demos the same model via the Geolocation API (`LiveLocationCard`: high-accuracy `watchPosition` + accuracy/age display).

---

## 🔨 Building & Testing from Source

### Prerequisites
- JDK 17
- Android SDK (API 26 to API 35) with Build Tools `35.0.0`
- Git

### Build Commands (Windows PowerShell)

```powershell
# 1. Run full unit test suite (all tests must pass)
.\gradlew.bat testDebugUnitTest

# 2. Build Debug APK
.\gradlew.bat assembleDebug
```

The generated APK will be output at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Automated Test Suite

Nari-Suraksha includes a battle-tested unit test suite covering **89 automated unit tests** across 15 test classes (100% pass rate):

| Test Class | Test Case | Description |
|---|---|---|
| `MeshNetworkSimulationTest` | `testThreeNodeDiscreteEventMeshPropagation` | Full 3-node simulation: A (Victim) -> B (Relay) -> C (Responder) with TTL decrement & return ACK |
| `RelayPolicyTest` | `testDeterministicForwardingAndRateBudget` | Forwarding rules, self-packet dropping, and 12-relays/min battery budget enforcement |
| `RelayDedupCacheTest` | `testThreadSafeLruEvictionAndLoopPrevention` | 60-second sliding deduplication cache prevents packet forwarding storms |
| `BleAdvertiserQueueTest` | `testPriorityQueuePreemption` | Verifies `OWN_SOS` (Priority 1) immediately preempts and cancels active mesh `RELAY` (Priority 3) |
| `PacketCodecTest` | `testEncodeDecodeRoundTrip` | 17-byte wire packet serialization/deserialization symmetry |
| `PacketCodecTest` | `testProtocolV2NibbleTtl` | Protocol V2 4-bit TTL bitmask flags verification |
| `PacketCodecTest` | `testLegacy15ByteDecodeFallback` | Backwards compatibility with legacy unversioned packets |
| `PacketCodecTest` | `testAckTargetSenderId` | Targeted ACK packet encoding and decoding |
| `PacketCodecTest` | `testLocationUnavailableFlag` | Fallback encoding when GPS coordinates are unavailable |
| `PacketCodecTest` | `testTestSosPacketFlag` | Non-emergency test beacon flag verification |
| `PacketCodecTest` | `testSafeZonesDistanceCalculation` | Haversine distance accuracy between known coordinates |
| `SosPacketTest` | `testDefaultSosPacketFlags` | Standard SOS flag bitmask verification |
| `SosPacketTest` | `testAckPacketFlags` | ACK flag bitmask verification |
| `SosPacketTest` | `testTestBroadcastPacketFlags` | Test broadcast flag bitmask verification |
| `SosPacketTest` | `testLocationUnavailableFlags` | Null coordinate handling and flag bitmask |
| `SosPacketTest` | `testTargetSenderIdPackingAndUnpacking` | Target sender ID 16-bit packing and extraction |
| `ShakeDetectionEngineTest` | `test0SpikesNoSos` | Baseline weak acceleration produces no trigger |
| `ShakeDetectionEngineTest` | `test1SpikeNoSos` | Single spike does not trigger SOS |
| `ShakeDetectionEngineTest` | `test2SpikesNoSos` | Two spikes do not trigger SOS |
| `ShakeDetectionEngineTest` | `test3ValidSpikesWithin2SecondsTriggersSos` | Three valid spikes within 2 seconds triggers SOS |
| `ShakeDetectionEngineTest` | `test3SpikesOutsideWindowNoTrigger` | Spikes outside 2-second window are pruned and do not trigger |
| `ShakeDetectionEngineTest` | `testSpikeDuringCooldownIgnored` | Spikes during 3-second cooldown are ignored |
| `ShakeDetectionEngineTest` | `testWeakAccelerationIgnored` | Normal movement (< 2.7g) produces 0 spikes |
| `ShakeDetectionEngineTest` | `testSingleBumpMechanicalOscillationsDebounced` | Table drops / single bounces within 220ms are debounced |
| `ShakeDetectionEngineTest` | `testSensorUnavailableGracefulHandling` | Missing accelerometer sensor fails gracefully without crashing |
| `SosManagerTest` | `testCountdownDurationIsThreeSeconds` | Verifies countdown duration is exactly 3 seconds |
| `SosManagerTest` | `testFullSosCountdownAndConfirmation` | Full 3-second countdown state machine and confirmation |
| `SosManagerTest` | `testCancelCountdownAbortsSos` | Cancelling countdown aborts broadcast and audio capture |
| `SosManagerTest` | `testDuplicateTriggerIgnoredDuringCountdown` | Duplicate triggers during countdown do not reset timer |
| `SosManagerTest` | `testStopSosStopsRecordingAndResets` | Stopping SOS resets state and halts audio recording |
| `SosManagerTest` | `testSosConfirmedEvenIfAudioFails` | Microphone initialization failure never blocks SOS broadcast |
| `BleDiagnosticsTrackerTest` | `testInitialCountersAreZero` | Diagnostic telemetry counters start at zero |
| `BleDiagnosticsTrackerTest` | `testCounterIncrements` | Telemetry counters increment accurately |
| `BleDiagnosticsTrackerTest` | `testDebugScanModeToggle` | Toggling software-filtered debug scan mode |
| `BleDiagnosticsTrackerTest` | `testRecordEventAndRingBufferCapacity` | Event ring buffer caps at 80 items without memory leaks |
| `AckHandshakeAndRateLimitTest` | `testTargetAckAcceptedForCorrectVictim` | Handshake ACK matching victim ID accepted |
| `AckHandshakeAndRateLimitTest` | `testResponderDeduplication` | Multiple ACKs from same responder count as 1 |
| `AckHandshakeAndRateLimitTest` | `testPerSenderRateLimit` | Per-sender 10-second spam window enforced |
| `AckHandshakeAndRateLimitTest` | `testGlobalAlertRateLimit` | Global 10 alerts/minute limit enforced |
| `SafeZonesRepositoryTest` | `testPreCachedSafeZonesNotEmpty` | Offline safe zones list loaded with valid coordinates |
| `SafeZonesRepositoryTest` | `testCategoryCoverage` | Police, Women Desk, Hospital, Metro categories present |
| `SafeZonesRepositoryTest` | `testCompassBearingCalculation` | 360-degree compass bearing calculation accuracy |
| `EmergencyContactEntityTest` | `testDefaultValues` | Room entity instantiation with default values |
| `EmergencyContactEntityTest` | `testPrimaryFlagMutation` | Immutable primary flag updating |
| `MeshCoreTest` | `testCrossTransportDuplicateDropped` | BLE+WiFi same broadcast counts once |
| `MeshCoreTest` | `testSosAlertsAndRelaysWithDecrementedTtl` | Shared pipeline alert + TTL-1 relay |
| `MeshCoreTest` | `testTtlExhaustedAlertsButNeverRelays` | TTL 0 delivered locally, never forwarded |
| `MeshCoreTest` | `testForeignAckRelayedWithoutAlert` | Foreign ACKs forward silently toward victim |
| `WifiEnvelopeTest` | `testWrapUnwrapRoundTripPreservesTtl` | UDP envelope preserves packet + TTL |
| `WifiEnvelopeTest` | `testGarbageRejected` | LAN noise never decodes into phantom SOS |
| `LocationFixTest` | `testHighQualityFreshAccurateFix` | HIGH tier for fresh ≤15 m fixes |
| `LocationFixTest` | `testStaleFixFlaggedRegardlessOfAccuracy` | Old fixes flagged STALE, never trusted |
| `LocationFixTest` | `testUnknownAccuracyNeverClaimsHigh` | Unknown precision never claims HIGH |

---

## 📱 Two-Phone Physical Device Manual Verification Procedure

To test end-to-end communication on two physical Android devices:

### Preparation
1. Install `app-debug.apk` on **Phone A** (Victim) and **Phone B** (Responder).
2. On both phones:
   - Launch **Calculator**.
   - Complete the permission setup (Grant Nearby Devices / Bluetooth, Location, Notifications).
   - Enter your custom 4-digit PIN (e.g., `1234`).
   - Long press `=` to open the main safety screen.
3. Open **Settings (gear icon)** -> tap **BLE DIAGNOSTICS & TEST MODE** on both phones:
   - Verify Bluetooth is **ON**.
   - Verify permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `LOCATION`) show **Granted**.
   - Verify Scanning shows **ACTIVE**.

### Test 1: Non-Emergency Diagnostic Broadcast
1. On **Phone A**: Tap **"START TEST BROADCAST"**.
2. On **Phone B**: Watch the live event log on the Diagnostics screen.
3. **Expected Result**: Phone B logs `NARI_BLE_PACKET_RECEIVED` with Phone A's Device ID and RSSI. Because this packet is flagged with `FLAG_TEST`, no loud siren or incoming emergency alert is triggered.

### Test 2: End-to-End Real SOS & 2-Way ACK Handshake
1. On **Phone A**: Tap the hero red **SOS BUTTON** (or enter PIN + `=` in Calculator).
2. **Phone A** displays the **3-second countdown** (`3... 2... 1...`).
3. After 3 seconds, Phone A confirms SOS and begins broadcasting distress beacons.
4. **Phone B** immediately triggers the full-screen **Incoming Alert** with vibration, approximate distance bucket, and siren.
5. On **Phone B**: Tap **"I AM RESPONDING (SEND ACK)"**.
6. **Phone B** broadcasts a targeted ACK addressed to Phone A.
7. On **Phone A**: The **BLE Responders** counter updates from `0` to `1`.
8. Tap **"I AM RESPONDING"** again on Phone B. Phone A's counter remains `1` (deduplication confirmed).
9. On **Phone A**: Tap **"I'M SAFE (STOP SOS)"** to halt the broadcast.

---

## ⚠️ Real-World Platform Limitations & Technical Disclosures

- **Android Background Limitations**: While `ScanForegroundService` uses `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`, aggressive OEM battery managers (Samsung OneUI sleeping apps, Xiaomi MIUI battery saver, Vivo/Oppo power management) can suppress background BLE scanning if battery optimization is not set to **"Unrestricted"**.
- **Bluetooth Hardware**: Devices with budget chipsets that lack BLE peripheral advertising support can scan and receive alerts, but cannot advertise SOS signals (the Diagnostics screen displays whether Advertiser is supported).
- **Offline Map Scope**: The tactical radar renders 100% offline using local Canvas vector graphics and pre-cached coordinates. Turn-by-turn routing opens external map applications if available.
- **Voice Calls & SMS**: BLE emergency networking operates with zero internet and zero cellular reception. Direct phone calling (`ACTION_DIAL`) and SMS require active cellular carrier service.

---

## 📄 License
This project was developed for **Hackinverse 1.0** by **Team Broken Coders**. Distributed for open safety and educational purposes.
