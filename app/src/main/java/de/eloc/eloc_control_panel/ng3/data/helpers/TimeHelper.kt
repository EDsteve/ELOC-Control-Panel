package de.eloc.eloc_control_panel.ng3.data.helpers

import android.content.Context
import de.eloc.eloc_control_panel.R

object TimeHelper {

    private const val ONE_MINUTE_SECONDS = 60.0
    private const val ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60
    private const val ONE_DAY_SECONDS = ONE_HOUR_SECONDS * 24

    fun formatHours(context: Context, hours: Double) =
        formatSeconds(context, toSeconds(hours))

    fun formatMillis(context: Context, millis: Number, useSeconds: Boolean = false) =
        formatSeconds(context, millis.toDouble() / 1000, useSeconds)

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

    fun toHours(seconds: Number) = seconds.toDouble() / ONE_HOUR_SECONDS

    fun toSeconds(hours: Number) = hours.toDouble() * ONE_HOUR_SECONDS
}