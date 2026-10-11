import React from 'react'
import { Shield, Download, Smartphone, Radio, MapPin, Heart, Github } from 'lucide-react'

export default function Header({ activeTab, setActiveTab }) {
  return (
    <header className="sticky top-0 z-40 bg-white border-b border-pink-border px-4 lg:px-8 py-3.5 transition-all shadow-sm">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        
        {/* Brand Logo & Subtitle */}
        <div 
          onClick={() => setActiveTab('home')}
          className="flex items-center gap-3 cursor-pointer group"
        >
          <div className="w-11 h-11 rounded-2xl bg-pink-primary flex items-center justify-center text-white shadow-sm group-hover:bg-pink-pressed transition-colors">
            <Shield className="w-6 h-6 stroke-[2.5]" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-extrabold tracking-tight text-ink-primary">
                Nari-Suraksha
              </h1>
              <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-pink-soft text-pink-primary border border-pink-border">
                100% Offline
              </span>
            </div>
            <p className="text-xs font-medium text-ink-secondary">
              Safety • Support • Empowerment
            </p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="hidden md:flex items-center gap-1 bg-bg-app p-1.5 rounded-full border border-pink-border">
          {[
            { id: 'home', label: 'Home', icon: Heart },
            { id: 'simulator', label: 'Mesh Simulator', icon: Radio },
            { id: 'features', label: 'Features', icon: Smartphone },
            { id: 'safezones', label: 'Safe Zones', icon: MapPin },
          ].map((tab) => {
            const Icon = tab.icon
            const isActive = activeTab === tab.id
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-bold transition-all ${
                  isActive
                    ? 'bg-pink-primary text-white shadow-sm'
                    : 'text-ink-secondary hover:text-pink-primary hover:bg-pink-soft'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-white' : 'text-pink-primary'}`} />
                {tab.label}
              </button>
            )
          })}
        </nav>

        {/* Action CTAs */}
        <div className="flex items-center gap-2.5">
          <a
            href="https://github.com/Hasnainyt/hackinverse-women-safety-sos"
            target="_blank"
            rel="noreferrer"
            className="hidden sm:flex items-center gap-1.5 px-3.5 py-2 rounded-btn text-xs font-bold text-ink-primary hover:text-black bg-pink-soft hover:bg-pink-soft/80 border border-pink-border transition-colors"
          >
            <Github className="w-4 h-4" />
            <span>GitHub</span>
          </a>

          <a
            href="https://github.com/Hasnainyt/hackinverse-women-safety-sos/raw/main/app-debug.apk"
            download
            className="flex items-center gap-2 px-4 py-2 rounded-btn text-xs font-bold text-white bg-pink-primary hover:bg-pink-pressed shadow-pink-action transition-all"
          >
            <Download className="w-4 h-4" />
            <span>Download APK</span>
          </a>
        </div>

      </div>
    </header>
  )
}
