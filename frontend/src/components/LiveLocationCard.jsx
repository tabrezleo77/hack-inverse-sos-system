import React, { useState, useEffect } from 'react'
import { MapPin, LocateFixed, AlertTriangle } from 'lucide-react'

/**
 * Live GPS demo using the browser Geolocation API (the web counterpart of the
 * Android GNSS tracker). High-accuracy watchPosition with honest precision:
 * coordinates are always shown WITH their accuracy radius and fix age —
 * never false precision.
 */
export default function LiveLocationCard() {
  const [fix, setFix] = useState(null)
  const [error, setError] = useState(null)
  const [watching, setWatching] = useState(false)

  useEffect(() => {
    if (!('geolocation' in navigator)) {
      setError('Geolocation is not supported in this browser.')
      return
    }
    setWatching(true)
    const id = navigator.geolocation.watchPosition(
      (pos) => {
        setFix({
          lat: pos.coords.latitude,
          lon: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
          timestamp: pos.timestamp,
        })
        setError(null)
      },
      (err) => {
        if (err.code === err.PERMISSION_DENIED) {
          setError('Location permission denied. Enable it to see live tracking.')
        } else if (err.code === err.TIMEOUT) {
          setError('GPS timeout — trying again (weak signal or indoors).')
        } else {
          setError('Position unavailable — showing last known fix if any.')
        }
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 5000 }
    )
    return () => {
      navigator.geolocation.clearWatch(id)
      setWatching(false)
    }
  }, [])

  const ageS = fix ? Math.max(0, Math.round((Date.now() - fix.timestamp) / 1000)) : null
  const quality =
    !fix ? 'NO FIX' :
    fix.accuracy <= 15 && ageS <= 15 ? 'HIGH' :
    fix.accuracy <= 50 && ageS <= 60 ? 'MEDIUM' : 'LOW'

  const qualityColor =
    quality === 'HIGH' ? 'text-status-safe' :
    quality === 'MEDIUM' ? 'text-status-guardian' :
    quality === 'LOW' ? 'text-status-amber' : 'text-ink-secondary'

  return (
    <div className="bg-bg-card rounded-card border border-pink-border shadow-flat-card p-5 sm:p-6">
      <div className="flex items-center gap-2.5 mb-1">
        <div className="w-9 h-9 rounded-full bg-pink-soft flex items-center justify-center text-pink-primary">
          <LocateFixed className="w-4.5 h-4.5" />
        </div>
        <div>
          <h3 className="font-extrabold text-ink-primary text-base leading-tight">Live Location Tracking</h3>
          <p className="text-[11px] text-ink-secondary font-medium">GNSS via Geolocation API • high-accuracy mode</p>
        </div>
        <span className={`ml-auto text-[11px] font-black ${qualityColor}`}>{quality}</span>
      </div>

      {fix ? (
        <div className="mt-3 space-y-1.5 font-mono text-sm text-ink-primary">
          <p>Lat: {fix.lat.toFixed(5)}&nbsp;&nbsp;Lon: {fix.lon.toFixed(5)}</p>
          <p className="flex items-center gap-1.5">
            <MapPin className="w-3.5 h-3.5 text-pink-primary" />
            ±{Math.round(fix.accuracy)} m accuracy • {ageS < 5 ? 'just now' : `${ageS} s ago`}
          </p>
          <a
            className="inline-block text-xs font-bold text-pink-primary hover:underline"
            href={`https://maps.google.com/?q=${fix.lat},${fix.lon}`}
            target="_blank"
            rel="noreferrer"
          >
            Open in Google Maps →
          </a>
          <p className="text-[11px] font-sans text-ink-secondary">
            {watching ? 'Updating every few seconds while this page is open.' : ''} Same data the SOS SMS sends to emergency contacts.
          </p>
        </div>
      ) : (
        <div className="mt-3 flex items-start gap-2 text-sm text-ink-secondary">
          <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0 text-status-amber" />
          <p>{error || 'Acquiring GPS fix…'}</p>
        </div>
      )}
    </div>
  )
}
