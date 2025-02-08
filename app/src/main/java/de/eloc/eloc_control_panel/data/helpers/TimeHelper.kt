package de.eloc.eloc_control_panel.data.helpers

import android.content.Context
import de.eloc.eloc_control_panel.R
import java.util.Calendar
import java.util.TimeZone

object TimeHelper {

    private const val ONE_MINUTE_SECONDS = 60.0
    private const val ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60
    private const val ONE_DAY_SECONDS = ONE_HOUR_SECONDS * 24

    val timeZoneOffsetHours: Int =
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

    fun prettyDate(epoch: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epoch
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        val sec = calendar.get(Calendar.SECOND)
        val sHr = if (hour < 10) {
            "0$hour"
        } else {
            "$hour"
        }
        val sMin = if (min < 10) {
            "0$min"
        } else {
            "$min"
        }
        val sSecs = if (sec < 10) {
            "0$sec"
        } else {
            "$sec"
        }
        return "$year/$month/$day $sHr:$sMin:$sSecs"
    }

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
}