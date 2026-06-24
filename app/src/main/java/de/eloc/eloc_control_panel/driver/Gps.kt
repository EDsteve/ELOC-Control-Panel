package de.eloc.eloc_control_panel.driver

/**
 * On-board GNSS status of the ELOC device (from getStatus -> "gps").
 *
 * Distinct from the *phone's* GPS (see LocationHelper / the gps_gauge in DeviceActivity, which gates
 * recording on the handset's location accuracy). This mirrors the device's own GPS module, which sets
 * the device clock from satellite UTC and derives the local timezone.
 */
class Gps {

    // True if GPS support is compiled into the running firmware at all.
    var present = false
        internal set

    // True if the module is powered and running this boot. A fresh-clock duty-cycle wake may skip
    // powering it, so this can be false even on a GPS-capable device.
    var powered = false
        internal set

    // True once the module has a valid position fix.
    var hasFix = false
        internal set

    var satellites = 0
        internal set

    // True once the device clock has been corrected from GPS UTC this power session.
    var timeSynced = false
        internal set

    fun reset() {
        present = false
        powered = false
        hasFix = false
        satellites = 0
        timeSynced = false
    }
}
