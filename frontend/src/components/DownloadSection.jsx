import React from 'react'
import { Download, CheckCircle, QrCode, Github, Sparkles } from 'lucide-react'
import { useI18n } from '../i18n'

export default function DownloadSection() {
  const i18n = useI18n()
  const t = i18n.download
  const downloadUrl = "https://github.com/Hasnainyt/hackinverse-women-safety-sos/raw/main/app-debug.apk"
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(downloadUrl)}`

  return (
    <section className="relative overflow-hidden rounded-card bg-white border-2 border-pink-border p-6 lg:p-10 shadow-flat-card space-y-6">
      
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
        
        {/* Left Col: Download details */}
        <div className="lg:col-span-8 space-y-5">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-pink-soft border border-pink-border">
            <Sparkles className="w-3.5 h-3.5 text-pink-primary" />
            <span className="text-xs font-bold text-pink-primary">
              {t.badge}
            </span>
          </div>

          <div>
            <h3 className="text-3xl sm:text-4xl font-black text-ink-primary">
              {t.title}
            </h3>
            <p className="text-base text-ink-secondary font-medium mt-2 max-w-xl leading-relaxed">
              {t.description}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3.5 pt-1">
            <a
              href={downloadUrl}
              download
              className="inline-flex items-center gap-2.5 px-6 py-4 rounded-btn text-base font-extrabold text-white bg-pink-primary hover:bg-pink-pressed active:scale-[0.98] shadow-pink-action transition-all"
            >
              <Download className="w-5 h-5 stroke-[2.5]" />
              <span>{t.downloadBtn}</span>
            </a>

            <a
              href="https://github.com/Hasnainyt/hackinverse-women-safety-sos"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 px-5 py-4 rounded-btn text-base font-bold text-ink-primary bg-white hover:bg-pink-soft border-2 border-pink-border shadow-sm transition-all"
            >
              <Github className="w-4 h-4" />
              <span>{t.githubBtn}</span>
            </a>
          </div>

          {/* Quick Install Checklist */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
            <div className="flex items-start gap-2.5 bg-bg-app p-3.5 rounded-2xl border border-pink-border">
              <CheckCircle className="w-4 h-4 text-status-safe flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-bold text-ink-primary">{t.steps.step1}</p>
                <p className="text-[11px] text-ink-secondary">{t.steps.step1Desc}</p>
              </div>
            </div>
            <div className="flex items-start gap-2.5 bg-bg-app p-3.5 rounded-2xl border border-pink-border">
              <CheckCircle className="w-4 h-4 text-status-safe flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-bold text-ink-primary">{t.steps.step2}</p>
                <p className="text-[11px] text-ink-secondary">{t.steps.step2Desc}</p>
              </div>
            </div>
            <div className="flex items-start gap-2.5 bg-bg-app p-3.5 rounded-2xl border border-pink-border">
              <CheckCircle className="w-4 h-4 text-status-safe flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-bold text-ink-primary">{t.steps.step3}</p>
                <p className="text-[11px] text-ink-secondary">{t.steps.step3Desc}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Col: QR Code for 1-Scan Mobile Download */}
        <div className="lg:col-span-4 flex flex-col items-center text-center">
          <div className="bg-bg-app p-5 rounded-[28px] border-2 border-pink-border shadow-sm">
            <img
              src={qrUrl}
              alt="Scan to download Nari-Suraksha APK"
              className="w-44 h-44 rounded-xl border border-pink-border bg-white p-1"
            />
            <p className="text-sm font-black text-ink-primary mt-3">
              {t.scanQr}
            </p>
            <p className="text-xs text-ink-secondary font-medium">
              {t.scanSub}
            </p>
          </div>
        </div>

      </div>

    </section>
  )
}
