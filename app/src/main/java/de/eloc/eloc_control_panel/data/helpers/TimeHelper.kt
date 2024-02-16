package de.eloc.eloc_control_panel.data.helpers

import android.content.Context
import android.os.SystemClock
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.interfaces.StringCallback
import de.eloc.eloc_control_panel.old.SNTPClient
import java.util.Calendar
import java.util.TimeZone

object TimeHelper {

    private const val ONE_MINUTE_SECONDS = 60.0
    private const val ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60
    private const val ONE_DAY_SECONDS = ONE_HOUR_SECONDS * 24

    private fun timeZoneOffsetHours() =
        (TimeZone.getDefault().rawOffset.toDouble() / (ONE_HOUR_SECONDS * 1000)).toInt()

    fun formatHours(context: Context, hours: Double) =
        formatSeconds(context, toSeconds(hours))

    fun formatMillis(context: Context, millis: Number, useSeconds: Boolean = false) =
        formatSeconds(context, millis.toDouble() / 1000, useSeconds)

    fun formatSeconds(
        context: Context,
        seconds: Int,
        useSeconds: Boolean = false
    ) = formatSeconds(context, seconds.toDouble(), useSeconds)

    private fun formatSeconds(
        context: Context,
        seconds: Double,
        useSeconds: Boolean = false
    ): String {
        val days = (seconds / ONE_DAY_SECONDS).toInt()
        var remaining = seconds - (days * ONE_DAY_SECONDS)
        val prettyDays = if (days > 0) "${days}d" else ""

        val hours = (remaining / ONE_HOUR_SECONDS).toInt()
        remaining -= toSeconds(hours)
        val prettyHours = if (hours > 0) "${hours}h" else ""

        val minutes = (remaining / ONE_MINUTE_SECONDS).toInt()
        remaining -= (minutes * ONE_MINUTE_SECONDS)
        val prettyMinutes = if (minutes > 0) "${minutes}m" else ""

        val result = "$prettyDays $prettyHours $prettyMinutes".trim()

        return result.ifEmpty {
            if (useSeconds) {
                "${remaining.toInt()}s"
            } else {
                context.getString(R.string.less_than_one_minute)
            }
        }
    }

    fun toSeconds(hours: Number) = hours.toDouble() * ONE_HOUR_SECONDS

    fun syncBoardClock(updateEloc: Boolean = false, callback: StringCallback? = null) {
        SNTPClient.getDate(
            5000,
            Calendar.getInstance().timeZone
        ) { _,
            _,
            googletimestampMillis,
            _
            ->
            run {
                val timeZoneOffsetHours = timeZoneOffsetHours()
                var differenceMillis = 0L
                var isLocal = false
                val timestamp = if (googletimestampMillis == 0L) {
                    // If we cannot get google time, use phone time
                    isLocal = true
                    System.currentTimeMillis()
                } else {
                    differenceMillis = System.currentTimeMillis() - googletimestampMillis
                    googletimestampMillis
                }
                if (updateEloc) {
                    val seconds = (googletimestampMillis / 1000.0).toLong()
                    DeviceDriver.syncTime(seconds, differenceMillis, timeZoneOffsetHours)
                }

                PreferencesHelper.instance.saveTimestamps(
                    SystemClock.elapsedRealtime(),
                    timestamp
                )

                val message = if (isLocal) {
                    App.instance.getString(R.string.sync_template_withno_difference)
                } else {
                    App.instance.getString(R.string.sync_template_with_difference, differenceMillis)
                }
                callback?.handler(message)
            }
        }
    }

    fun prettify(s: String?): String {
        val dirty = s ?: ""
        val parts = dirty.split("-")
        if (parts.size >= 7) {
            val date = "${parts[0]}/${parts[1]}/${parts[2]}"
            val time = "${parts[3]}:${parts[4]}:${parts[5]}"
            val timeZone = parts[6]
                .replace("+0", " +")
                .replace(":00", "")
            return "$date $time $timeZone"
        }
        return ""
    }
}