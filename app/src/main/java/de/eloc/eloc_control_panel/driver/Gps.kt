package de.eloc.eloc_control_panel.driver

/**
 * On-board GNSS status of the ELOC device (from getStatus -> "gps").
 *
 * Distinct from the *phone's* GPS (see LocationHelper / the gps_gauge in DeviceActivity, which gates
 * recording on the handset's location accuracy). This mirrors the device's own GPS module, which sets
 * the device clock from satellite UTC and derives the local timezone.
 */
class Gps {

    companion object {
        // Estimated horizontal accuracy ≈ HDOP × UERE for a single-frequency GNSS receiver.
        const val UERE_METERS = 5.0
    }

    // True if GPS support is compiled into the running firmware at all.
    var present = false
        internal set

    // True if the module is powered and running this boot. A fresh-clock duty-cycle wake may skip
    // powering it, so this can be false even on a GPS-capable device.
    var powered = false
        internal set

    // True once the module has a valid position fix. Live-gated on firmware ≥1.54 (reflects the
    // CURRENT fix state); latched on older firmware (stays true once a fix was ever seen).
    var hasFix = false
        internal set

    var satellites = 0
        internal set

    // True once the device clock has been corrected from GPS UTC this power session.
    var timeSynced = false
        internal set

    // Seconds since the device's last position fix (-1 = none / firmware too old to report).
    var fixAgeS = -1
        internal set

    // Horizontal dilution of precision of the live solution (0.0 = no live solution / old firmware).
    var hdop = 0.0
        internal set

    // Last-known position from the device's own GNSS, or null when the firmware reports none.
    // Latched like the firmware's lat/lon fields: present whenever the device has had a fix this
    // power session (so may be non-null while the live `hasFix` above is false).
    var latitude: Double? = null
        internal set

    var longitude: Double? = null
        internal set

    // Estimated ELOC horizontal accuracy in meters, or -1.0 when there is no live solution.
    val accuracyMeters: Double get() = if (hdop > 0.0) hdop * UERE_METERS else -1.0

    // True when the device has reported real coordinates that could be recorded as the ELOC location.
    val hasLocation: Boolean get() = latitude != null && longitude != null

    fun reset() {
        present = false
        powered = false
        hasFix = false
        satellites = 0
        timeSynced = false
        fixAgeS = -1
        hdop = 0.0
        latitude = null
        longitude = null
    }
}
