package de.eloc.eloc_control_panel.driver

/**
 * Ordering of ELOC firmware version strings (Phase 3 of the firmware-update plan).
 *
 * A device version looks like `ELOC-P_V1.67`: a prefix naming the build line
 * followed by a dotted numeric tail. Only the tail is ordered; the prefix must
 * match **exactly**, so a Patrol build is never offered to a mainline device on
 * a string-sort accident. Anything that does not parse yields `null`, and every
 * caller treats `null` as "offer nothing" — a device whose version string the
 * app cannot read simply keeps the file picker.
 *
 * This decides only whether to *offer* an update. [FirmwareUpdater]'s rollback
 * verdict stays the plain equality check it is, so a deliberate downgrade
 * through the picker is still reported as a success.
 */
object FirmwareVersion {

    /**
     * The oldest firmware a Bluetooth update can be started *from*: the first
     * version that advertises `fwUpdateProto` in `getStatus`. Reverting below
     * this is a one-way trip that needs an SD-card swap to undo — the picker
     * warns about it but never blocks it.
     *
     * A constant in the app on purpose, not a field in the release metadata:
     * it describes what the app's own protocol needs, not what a release claims.
     */
    const val MIN_OTA_SOURCE_VERSION = "ELOC-P_V1.47"

    // Prefix is non-greedy so the numeric tail is as long as possible:
    // "ELOC-P_V1.67" -> ("ELOC-P_V", "1.67").
    private val pattern = Regex("""^(.*?)(\d+(?:\.\d+)*)$""")

    // The tail is compared component-wise, not as a decimal, so the firmware's
    // two-digit convention has to hold: V1.70, never V1.7 (which would compare
    // as older than V1.67). Every release so far is tagged that way.

    data class Parsed(val prefix: String, val numbers: List<Int>)

    fun parse(version: String): Parsed? {
        val match = pattern.matchEntire(version.trim()) ?: return null
        val numbers = match.groupValues[2].split('.').map {
            it.toIntOrNull() ?: return null
        }
        return Parsed(match.groupValues[1], numbers)
    }

    /**
     * Negative / zero / positive when [a] is older / the same as / newer than
     * [b]. Null when either side does not parse or the two build lines differ.
     */
    fun compare(a: String, b: String): Int? {
        val left = parse(a) ?: return null
        val right = parse(b) ?: return null
        if (left.prefix != right.prefix) {
            return null
        }
        val count = maxOf(left.numbers.size, right.numbers.size)
        for (i in 0 until count) {
            val leftPart = left.numbers.getOrElse(i) { 0 }
            val rightPart = right.numbers.getOrElse(i) { 0 }
            if (leftPart != rightPart) {
                return if (leftPart < rightPart) -1 else 1
            }
        }
        return 0
    }

    /** True only when [candidate] is a strictly newer build of the same line. */
    fun isNewer(candidate: String, installed: String): Boolean {
        val result = compare(candidate, installed)
        return (result != null) && (result > 0)
    }

    /** True only when [version] is definitely older than [MIN_OTA_SOURCE_VERSION]. */
    fun isBelowOtaSource(version: String): Boolean {
        val result = compare(version, MIN_OTA_SOURCE_VERSION)
        return (result != null) && (result < 0)
    }
}
