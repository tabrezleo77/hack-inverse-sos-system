# 🌸 Nari-Suraksha Web Frontend & Live Interactive Demo

[![Live Demo Web App](https://img.shields.io/badge/Live%20Demo-Vercel%20App-ff2d78?style=for-the-badge&logo=vercel&logoColor=white)](https://hackinverse-women-safety-sos-fronte.vercel.app/)

A modern, responsive React + Tailwind CSS landing page and interactive simulator matching the Nari-Suraksha aesthetic.

🌐 **Live Deployed App:** [https://hackinverse-women-safety-sos-fronte.vercel.app/](https://hackinverse-women-safety-sos-fronte.vercel.app/)

---

## ✨ Features Included
- **Hero & Branding:** "Your Safety Our Priority ♡" with pink gradient shield aesthetics.
- **4 Core Safety Cards:** Emergency, Detection, Broadcast, Community.
- **Interactive 2-Phone BLE Simulator:**
  - Phone A (Sender): SOS Button trigger, 5s countdown, Cancel option, Live responder ACK counter.
  - Phone B (Receiver): Screen-off alert detection, RSSI proximity meter, siren, strobe flashlight, and 2-way ACK transmission.
  - Decoy Mode: Working interactive calculator with secret PIN trigger (`1234=`).
- **Offline Architecture Engine:** Breakdown of the 15-byte compact BLE packet specification.
- **Pre-Cached Safe Zones Directory:** 24x7 Women Help Desks, Police Stations, and Hospitals with 1-tap dialers.
- **Direct APK Download Card:** Direct download link + QR code for instant mobile installation.
- **Sticky Bottom Alert Bar:** *"You are not alone. Help is just a tap away."* + Stay Safe button.

---

## 🚀 How to Run Locally

```bash
# 1. Navigate to frontend directory
cd frontend

# 2. Install dependencies
npm install

# 3. Start development server
npm run dev
```

Open your browser at `http://localhost:3000` to view and interact with the application.

---

## 📦 Build for Production

```bash
npm run build
```
The optimized production bundle will be generated in `frontend/dist/`.
