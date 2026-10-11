/**
 * Language-ready internationalization structure for Nari-Suraksha Web.
 * Clean English key-value dictionary ready for future language extensions.
 */

export const translations = {
  en: {
    app: {
      name: "Nari-Suraksha",
      tagline: "Network gayab, fir bhi help alive.",
      subtitle: "Offline-first women's safety with Bluetooth Low Energy mesh flooding.",
    },
    nav: {
      simulator: "Mesh Simulator",
      features: "Features",
      architecture: "Architecture",
      safezones: "Safe Zones",
      download: "Download APK",
    },
    simulator: {
      badge: "Visual Centerpiece • BLE Flooding Mesh",
      title: "3-Phone Offline BLE Mesh Simulator",
      subtitle: "Observe a distress packet traverse Phone A (Victim) through Phone B (Relay) to reach Phone C (Responder), and an ACK packet return.",
      disableRelayBtn: "Disable Relay Phone B (Break Link)",
      enableRelayBtn: "Enable Relay Phone B (Restore Mesh)",
      resetBtn: "Reset Simulation",
      relayDisabledNotice: "Relay Phone B is disabled. Distress packets cannot bridge the distance to Phone C unless in direct physical range. This demonstrates why the peer-to-peer flooding mesh saves lives.",
      relayEnabledNotice: "Multi-hop relay mesh active. Packets automatically hop across nearby devices without internet, cellular, or pairing.",
      phoneA: {
        title: "Phone A (Victim / Sender)",
        role: "Distress Originator",
        holdForHelp: "HOLD FOR HELP",
        countdown: "Emergency beacon starting in",
        cancel: "Cancel SOS",
        broadcasting: "Broadcasting SOS Beacon",
        meshLine: "Nearby phones are passing your alert along.",
        imSafe: "I'm Safe (Stop Broadcast)",
        respondersZero: "0 people responding",
        respondersActive: "1 person responding (ACK Received)",
        ackNotice: "A nearby phone acknowledged your signal.",
      },
      phoneB: {
        title: "Phone B (Relay Node)",
        role: "Multi-hop Mesh Forwarder",
        statusActive: "Mesh Relay Active",
        statusDisabled: "Relay Disabled (Broken Link)",
        waiting: "Listening for nearby BLE distress beacons...",
        forwarding: "Relaying packet (+1 hop, TTL decrement)...",
        packetsSent: "Packets Sent",
        packetsRecv: "Packets Recv",
        hopInfo: "Hops: 1 -> 2 | Deduplicated",
      },
      phoneC: {
        title: "Phone C (Guardian / Responder)",
        role: "Distress Receiver",
        status: "Guardian Scanner Active",
        waiting: "Scanning for nearby emergency signals...",
        alertTitle: "DISTRESS ALERT DETECTED",
        distanceBucket: "Within 3 m (Immediate)",
        hopsBadge: "Passed along 1 hop via Phone B",
        respondingBtn: "I AM RESPONDING (SEND ACK)",
        ackSentBtn: "✓ ACK TRANSMITTED TO VICTIM",
        sirenBtn: "Deterrent Siren",
        torchBtn: "Strobe Flashlight",
      },
      links: {
        link1: "Hop 1: A -> B (TTL: 3)",
        link2: "Hop 2: B -> C (TTL: 2)",
        ackLink: "Return: 2-Way ACK (C -> B -> A)",
      }
    },
    download: {
      badge: "Standalone Android Build (v1.0.0)",
      title: "Download Nari-Suraksha App",
      description: "Direct APK download for Android phones. Operates 100% offline with zero cloud telemetry, zero account required.",
      downloadBtn: "Download APK (18.3 MB)",
      githubBtn: "View Source on GitHub",
      scanQr: "Scan with Phone Camera",
      scanSub: "Direct mobile installation",
      steps: {
        step1: "Step 1: Download APK",
        step1Desc: "Save package directly to device",
        step2: "Step 2: Allow Unknown Sources",
        step2Desc: "Enable installation in Android settings",
        step3: "Step 3: 100% Offline Protected",
        step3Desc: "Functions in full airplane mode",
      }
    }
  }
}

export function useI18n(lang = 'en') {
  return translations[lang] || translations.en
}
