import React from 'react'
import { Shield, ArrowRight, Radio, Heart, Sparkles, CheckCircle } from 'lucide-react'

export default function HeroBanner({ onOpenSimulator }) {
  return (
    <section className="relative overflow-hidden rounded-card bg-white border-2 border-pink-border p-6 lg:p-10 shadow-flat-card">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
        
        {/* Left Column: Heading & CTAs */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Tag Pill */}
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-pink-soft border border-pink-border">
            <Shield className="w-4 h-4 text-pink-primary" />
            <span className="text-xs font-bold text-pink-primary tracking-wide">
              Women's Safety • Offline BLE Flooding Mesh
            </span>
          </div>

          {/* Main Title - Solid Colors Only, No Gradients */}
          <div>
            <h2 className="text-4xl sm:text-5xl lg:text-6xl font-black text-ink-primary tracking-tight leading-[1.1]">
              Your Safety, <br />
              <span className="text-pink-primary inline-flex items-center gap-3">
                Our Priority
                <Heart className="w-9 h-9 text-pink-primary inline stroke-[2.5]" />
              </span>
            </h2>
            <p className="mt-4 text-base sm:text-lg text-ink-secondary max-w-xl font-medium leading-relaxed">
              Stay safe. Stay connected. <strong className="text-pink-primary font-bold">Network gayab, fir bhi help alive.</strong> An offline guardian that broadcasts distress signals peer-to-peer across bystander phones without internet, cellular towers, or cloud servers.
            </p>
          </div>

          {/* Hero Action Buttons */}
          <div className="flex flex-wrap items-center gap-3.5 pt-1">
            <button
              onClick={onOpenSimulator}
              className="inline-flex items-center gap-2.5 px-6 py-4 rounded-btn text-base font-extrabold text-white bg-pink-primary hover:bg-pink-pressed active:scale-[0.98] shadow-pink-action transition-all"
            >
              <span className="px-2 py-0.5 rounded-md bg-white/20 text-xs font-black">SOS</span>
              <span>Launch Mesh Simulator</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <a
              href="https://github.com/Hasnainyt/hackinverse-women-safety-sos/raw/main/app-debug.apk"
              download
              className="inline-flex items-center gap-2 px-5 py-4 rounded-btn text-base font-bold text-pink-primary bg-white hover:bg-pink-soft border-2 border-pink-primary shadow-sm transition-all"
            >
              <span>Download APK (18.4 MB)</span>
            </a>
          </div>

          {/* Quick Value Metrics with Solid Cards */}
          <div className="grid grid-cols-3 gap-3 pt-3 max-w-md">
            <div className="bg-bg-app rounded-2xl p-3 border border-pink-border text-center">
              <p className="text-2xl font-black text-pink-primary">0 MB</p>
              <p className="text-[11px] font-bold text-ink-secondary mt-0.5">Internet Needed</p>
            </div>
            <div className="bg-bg-app rounded-2xl p-3 border border-pink-border text-center">
              <p className="text-2xl font-black text-pink-primary">3 Sec</p>
              <p className="text-[11px] font-bold text-ink-secondary mt-0.5">Cancel Window</p>
            </div>
            <div className="bg-bg-app rounded-2xl p-3 border border-pink-border text-center">
              <p className="text-2xl font-black text-pink-primary">17 Byte</p>
              <p className="text-[11px] font-bold text-ink-secondary mt-0.5">BLE Packet</p>
            </div>
          </div>

        </div>

        {/* Right Column: Flat Clean Shield Showcase Card */}
        <div className="lg:col-span-5 flex justify-center">
          <div className="w-full max-w-[340px] rounded-[36px] bg-bg-app p-8 border-2 border-pink-border shadow-flat-card flex flex-col items-center justify-center text-center space-y-5">
            
            <div className="w-32 h-32 rounded-full bg-pink-soft border-4 border-white flex items-center justify-center text-pink-primary shadow-sm">
              <Shield className="w-16 h-16 stroke-[2.2]" />
            </div>

            <div className="space-y-1.5">
              <span className="text-xs font-bold uppercase tracking-wider text-pink-primary bg-pink-soft px-3 py-1 rounded-full border border-pink-border">
                100% Offline Guardian
              </span>
              <h4 className="text-xl font-black text-ink-primary pt-2">
                Safety Builds Confidence
              </h4>
              <p className="text-xs text-ink-secondary font-medium leading-relaxed">
                Autonomous background BLE scanner runs with negligible battery drain and zero telemetry tracking.
              </p>
            </div>

            <div className="w-full pt-3 border-t border-pink-border flex items-center justify-around text-xs font-bold text-ink-secondary">
              <span className="flex items-center gap-1 text-status-safe">
                <CheckCircle className="w-3.5 h-3.5" />
                No SIM Needed
              </span>
              <span className="flex items-center gap-1 text-status-safe">
                <CheckCircle className="w-3.5 h-3.5" />
                No Cloud Account
              </span>
            </div>

          </div>
        </div>

      </div>
    </section>
  )
}
