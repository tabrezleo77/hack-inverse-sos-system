package com.brokencoders.narisuraksha.core

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

/**
 * Generates and persists a random anonymous 16-bit Short ID for the user's device.
 * No personally identifiable info, phone numbers, or names are ever broadcast.
 */
class DeviceIdProvider(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nari_device_id_prefs", Context.MODE_PRIVATE)

    val deviceId: Short by lazy {
        var id = try {
            prefs.getInt(KEY_DEVICE_ID, 0)
        } catch (e: ClassCastException) {
            // Key drifted type from an older build — wipe and regenerate.
            try {
                prefs.edit().remove(KEY_DEVICE_ID).apply()
            } catch (_: Exception) {
                // ignore cleanup failures
            }
            0
        }
        if (id == 0) {
            val random = SecureRandom()
            // generate non-zero 16-bit short value
            do {
                id = random.nextInt(0xFFFF) - 0x7FFF
            } while (id == 0)
            // commit() (not apply()) for the first write so a racing app+service
            // process cannot both read 0 and generate diverging IDs.
            try {
                prefs.edit().putInt(KEY_DEVICE_ID, id).commit()
            } catch (_: Exception) {
                try {
                    prefs.edit().putInt(KEY_DEVICE_ID, id).apply()
                } catch (_: Exception) {
                    // ignore persistence failures; id still returned for this session
                }
            }
        }
        id.toShort()
    }

    companion object {
        private const val KEY_DEVICE_ID = "anonymous_device_id"
    }
}
