package de.eloc.eloc_control_panel.activities

import androidx.annotation.StringRes
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.driver.DutyCycle
import de.eloc.eloc_control_panel.driver.Inference
import de.eloc.eloc_control_panel.driver.LoraWan

enum class TimeUnit(val seconds: Int, @StringRes val labelRes: Int) {
    SECONDS(1, R.string.time_unit_seconds),
    MINUTES(60, R.string.time_unit_minutes),
    HOURS(3600, R.string.time_unit_hours),
    DAYS(86400, R.string.time_unit_days),
}

data class QuickPick(val seconds: Int, val label: String)

object TimeSliderHelper {

    val ALL_QUICK_PICKS = listOf(
        QuickPick(1, "1s"),
        QuickPick(5, "5s"),
        QuickPick(10, "10s"),
        QuickPick(20, "20s"),
        QuickPick(30, "30s"),
        QuickPick(60, "1m"),
        QuickPick(120, "2m"),
        QuickPick(300, "5m"),
        QuickPick(600, "10m"),
        QuickPick(900, "15m"),
        QuickPick(1800, "30m"),
        QuickPick(3600, "1h"),
        QuickPick(6 * 3600, "6h"),
        QuickPick(12 * 3600, "12h"),
        QuickPick(86400, "1d"),
        QuickPick(2 * 86400, "2d"),
        QuickPick(3 * 86400, "3d"),
    )

    private val TIME_PROPERTIES = setOf(
        LoraWan.UPLINK_INTERVAL,
        Inference.OBS_WINDOW_SECS,
        DutyCycle.SLEEP_DURATION_S,
        DutyCycle.AWAKE_DURATION_S,
    )

    fun isTimeProperty(property: String): Boolean = property in TIME_PROPERTIES

    fun picksFor(min: Int, max: Int): List<QuickPick> =
        ALL_QUICK_PICKS.filter { it.seconds in min..max }

    fun unitsFor(min: Int, max: Int): List<TimeUnit> {
        val filtered = TimeUnit.entries.filter { unit ->
            val largestWholeUnitValue = (max / unit.seconds) * unit.seconds
            unit.seconds <= max && largestWholeUnitValue >= min
        }
        return filtered.ifEmpty { listOf(TimeUnit.SECONDS) }
    }

    fun bestUnitFor(seconds: Int): TimeUnit = when {
        seconds >= 86400 && seconds % 86400 == 0 -> TimeUnit.DAYS
        seconds >= 3600 && seconds % 3600 == 0 -> TimeUnit.HOURS
        seconds >= 60 && seconds % 60 == 0 -> TimeUnit.MINUTES
        else -> TimeUnit.SECONDS
    }

    fun toSeconds(value: Int, unit: TimeUnit): Int = value * unit.seconds

    fun toUnitValue(seconds: Int, unit: TimeUnit): Int = seconds / unit.seconds
}
