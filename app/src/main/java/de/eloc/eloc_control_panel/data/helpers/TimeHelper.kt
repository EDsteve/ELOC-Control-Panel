package de.eloc.eloc_control_panel.data.helpers

import android.content.Context
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.driver.DeviceDriver
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

    fun prettyDate(epoch: Long) : String{
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epoch
        val year = calendar.get(Calendar.YEAR)
        val month  = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        val sec = calendar.get(Calendar.SECOND)
        var sHr = if (hour < 10)  { "0$hour" }  else { "$hour" }
        var sMin = if (min < 10)  { "0$min" }  else { "$min" }
        var sSecs = if (sec < 10)  { "0$sec" }  else { "$sec" }
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

    fun syncBoardClock() {
         val unixTimeMillis = System.currentTimeMillis()
        val seconds = unixTimeMillis / 1000
        DeviceDriver.syncTime(seconds, timeZoneOffsetHours())
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