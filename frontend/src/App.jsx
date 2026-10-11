import React, { useState } from 'react'
import Header from './components/Header'
import HeroBanner from './components/HeroBanner'
import FeatureGrid from './components/FeatureGrid'
import InteractiveSimulator from './components/InteractiveSimulator'
import OfflineArchitecture from './components/OfflineArchitecture'
import SafeZonesExplorer from './components/SafeZonesExplorer'
import LiveLocationCard from './components/LiveLocationCard'
import DownloadSection from './components/DownloadSection'
import BottomBar from './components/BottomBar'
import { Shield, Heart, Github, ExternalLink } from 'lucide-react'

export default function App() {
  const [activeTab, setActiveTab] = useState('home')

  const scrollToSection = (tabId) => {
    setActiveTab(tabId)
    const el = document.getElementById(tabId)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  return (
    <div className="min-h-screen bg-bg-app text-ink-primary flex flex-col justify-between pb-24">
      
      {/* Top Header */}
      <Header activeTab={activeTab} setActiveTab={scrollToSection} />

      {/* Main Content Container */}
      <main className="max-w-7xl mx-auto px-4 lg:px-8 py-8 space-y-12">
        
        {/* Hero Banner */}
        <div id="home">
          <HeroBanner onOpenSimulator={() => scrollToSection('simulator')} />
        </div>

        {/* 3-Phone BLE Mesh Simulator (Visual Centerpiece) */}
        <div id="simulator">
          <InteractiveSimulator />
        </div>

        {/* 4 Core Features Flow */}
        <div id="features">
          <FeatureGrid onSelectFeature={(featId) => {
            if (featId === 'decoy' || featId === 'emergency' || featId === 'broadcast') {
              scrollToSection('simulator')
            } else if (featId === 'safezones') {
              scrollToSection('safezones')
            }
          }} />
        </div>

        {/* 100% Offline BLE Architecture */}
        <OfflineArchitecture />

        {/* Pre-Cached Safe Zones & Help Desks */}
        <div id="safezones">
          <SafeZonesExplorer />
        </div>

        {/* Live GNSS tracking demo (Geolocation API, high-accuracy) */}
        <div id="live-location">
          <LiveLocationCard />
        </div>

        {/* Download APK Section */}
        <div id="download">
          <DownloadSection />
        </div>

      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-pink-border py-8 px-4 lg:px-8 mt-12 text-center text-xs font-medium text-ink-secondary">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-pink-primary flex items-center justify-center text-white">
              <Shield className="w-3.5 h-3.5" />
            </div>
            <span className="font-extrabold text-ink-primary">Nari-Suraksha</span>
            <span className="text-ink-secondary">• Hackinverse 1.0</span>
          </div>

          <p className="flex items-center gap-1">
            Engineered with <Heart className="w-3.5 h-3.5 text-pink-primary fill-pink-primary inline" /> by <strong>Team Broken Coders</strong>
          </p>

          <a
            href="https://github.com/Hasnainyt/hackinverse-women-safety-sos"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-ink-primary hover:text-pink-primary font-bold"
          >
            <Github className="w-4 h-4" />
            <span>GitHub Repository</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </footer>

      {/* Persistent Bottom Bar */}
      <BottomBar onTriggerQuickDemo={() => scrollToSection('simulator')} />

    </div>
  )
}
