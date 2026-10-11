package com.brokencoders.narisuraksha

import com.brokencoders.narisuraksha.capture.FixQuality
import com.brokencoders.narisuraksha.capture.LocationFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards honest precision: quality tiers, staleness, and human-readable
 * accuracy strings. Coordinates must never be shown without their estimate.
 */
class LocationFixTest {

    private val now = 1_700_000_000_000L

    @Test
    fun testHighQualityFreshAccurateFix() {
        val fix = LocationFix(28.6, 77.2, 8f, now - 5_000, "gps")
        assertEquals(FixQuality.HIGH, fix.quality(now))
    }

    @Test
    fun testMediumQualityForModerateAccuracy() {
        val fix = LocationFix(28.6, 77.2, 35f, now - 30_000, "network")
        assertEquals(FixQuality.MEDIUM, fix.quality(now))
    }

    @Test
    fun testLowQualityForPoorAccuracy() {
        val fix = LocationFix(28.6, 77.2, 200f, now - 10_000, "network")
        assertEquals(FixQuality.LOW, fix.quality(now))
    }

    @Test
    fun testStaleFixFlaggedRegardlessOfAccuracy() {
        val fix = LocationFix(28.6, 77.2, 5f, now - 300_000, "gps")
        assertEquals(FixQuality.STALE, fix.quality(now))
    }

    @Test
    fun testUnknownAccuracyNeverClaimsHigh() {
        val fix = LocationFix(28.6, 77.2, null, now - 5_000, "cached")
        assertEquals(FixQuality.LOW, fix.quality(now))
        assertTrue(fix.describe(now).contains("accuracy unknown"))
    }

    @Test
    fun testDescribeShowsAccuracyAndAge() {
        val fix = LocationFix(28.6, 77.2, 12.4f, now - 9_000, "gps")
        val text = fix.describe(now)
        assertTrue(text.contains("±12 m"))
        assertTrue(text.contains("GPS"))
        assertTrue(text.contains("9 s ago"))
    }

    @Test
    fun testMapsLinkFormat() {
        val fix = LocationFix(28.6139, 77.2090, 10f, now, "gps")
        assertEquals("https://maps.google.com/?q=28.6139,77.209", fix.mapsLink())
    }
}
