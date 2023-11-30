package de.eloc.eloc_control_panel.ng3.data

import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

class ElocDeviceInfo(
    val plusCode: String,
    val name: String,
    val batteryVolts: Double,
    val ranger: String,
    val time: String,
    val recTime: Double,
    val accuracy: Double
) {
    companion object {

        fun parseForRanger(json: String, rangerName: String): ArrayList<ElocDeviceInfo> {
            val deviceInfos = ArrayList<ElocDeviceInfo>()

            try {
                val obj = JSONObject(json)
                val deviceArray = obj.getJSONArray("device_infos")
                for (i in 0 until deviceArray.length()) {
                    val jsonInfo = deviceArray.getJSONObject(i)
                    val ranger = jsonInfo.getString("ranger_name")
                    if (ranger != rangerName) {
                        // Only get devices for ranger with matched name.
                        continue
                    }

                    val plusCode = jsonInfo.getString("plus_code")
                    val deviceName = jsonInfo.getString("device_name")

                    val time = jsonInfo.getString("time")
                    var batteryVolts = 0.0
                    try {
                        val tmp = jsonInfo.getString("battery_volts")
                        batteryVolts = tmp.toDouble()
                    } catch (_: NumberFormatException) {

                    }
                    var recTime = 0.0
                    try {
                        val tmp = jsonInfo.getString("record_time_since_boot")
                        recTime = tmp.toDouble()
                    } catch (_: NumberFormatException) {

                    }

                    var accuracy = 0.0
                    try {
                        val tmp = jsonInfo.getString("gps_accuracy")
                        accuracy = tmp.toDouble()
                    } catch (ignore: NumberFormatException) {

                    }

                    val deviceInfo = ElocDeviceInfo(
                        plusCode,
                        deviceName,
                        batteryVolts,
                        ranger,
                        time,
                        recTime,
                        accuracy
                    )
                    deviceInfos.add(deviceInfo)
                }
            } catch (_: JSONException) {

            }

            return deviceInfos
        }
    }
}
