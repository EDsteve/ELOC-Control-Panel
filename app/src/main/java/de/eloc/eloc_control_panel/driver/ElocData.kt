package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.data.DeviceState
import de.eloc.eloc_control_panel.data.GainType
import de.eloc.eloc_control_panel.data.helpers.JsonHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import org.json.JSONObject

internal object ElocData {
    private const val KEY_SESSION = "session"
    private const val KEY_PAYLOAD = "payload"
    private const val KEY_MICROPHONE = "mic"
    private const val KEY_MICROPHONE_SAMPLE_RATE = "MicSampleRate"
    private const val PATH_SEPARATOR = JsonHelper.PATH_SEPARATOR
    private const val KEY_IDENTIFIER = "identifier"
    private const val KEY_MICROPHONE_TYPE = "MicType"
    private const val KEY_MICROPHONE_GAIN = "MicBitShift"
    private const val KEY_CONFIG = "config"
    private const val KEY_SECONDS_PER_FILE = "secondsPerFile"
    private const val KEY_LOCATION_CODE = "locationCode"
    private const val KEY_DEVICE = "device"
    private const val KEY_BLUETOOTH_ENABLED_DURING_REC = "bluetoothEnableDuringRecord"
    private const val KEY_FILE_HEADER = "fileHeader"
    private const val KEY_RECORDING_TIME = "recordingTime[h]"
    private const val KEY_TOTAL_RECORDING_TIME = "totalRecordingTime[h]"
    private const val KEY_FIRMWARE = "firmware"
    private const val KEY_UPTIME = "Uptime[h]"
    private const val KEY_BATTERY = "battery"
    private const val KEY_BATTERY_LEVEL = "SoC[%]"
    private const val KEY_TYPE = "type"
    private const val KEY_VOLTAGE = "voltage[V]"
    private const val KEY_SDCARD_SIZE = "SdCardSize[GB]"
    private const val KEY_SDCARD_FREE_GB = "SdCardFreeSpace[GB]"
    private const val KEY_SDCARD_FREE_PERC = "SdCardFreeSpace[%]"
    private const val KEY_STATE = "state"
    private const val KEY_RECORDING_STATE = "recordingState"
    private const val KEY_LOG_CONFIG = "logConfig"
    private const val KEY_LOG_TO_SD_CARD = "logToSdCard"

    var logToSDCardEnabled = false
        private set

    var recHoursSinceBoot = 0.0
        private set

    var sampleRate = 0.0
        private set

    var microphoneType = ""
        private set

    var sessionId = ""
        private set

    var microphoneGain = GainType.Low
        private set

    var secondsPerFile = 0.0
        private set

    var lastLocation = ""
        private set

    var bluetoothEnabledDuringRecording = false
        private set

    var fileHeader = ""
        private set

    private var recordingSeconds = 0.0

    var version = ""
        private set

    var uptimeHours = 0.0
        private set

    var batteryLevel = 0.0
        private set

    var batteryVoltage = 0.0
        private set

    var batteryType = ""
        private set

    var freeSpaceGb = 0.0
        private set

    var freeSpacePercentage = 0.0
        private set

    var sdCardSizeGb = 0.0
        private set

    var deviceState = DeviceState.Idle
        private set

    fun parseConfig(jsonObject: JSONObject) {
        val sampleRatePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_SAMPLE_RATE"
        sampleRate = JsonHelper.getJSONNumberAttribute(sampleRatePath, jsonObject)

        val typePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_TYPE"
        microphoneType = JsonHelper.getJSONStringAttribute(typePath, jsonObject)

        val gainPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_GAIN"
        val micBitShift = JsonHelper.getJSONStringAttribute(gainPath, jsonObject)
        microphoneGain = GainType.fromValue(micBitShift)

        val secondsPerFilePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_SECONDS_PER_FILE"
        secondsPerFile = JsonHelper.getJSONNumberAttribute(secondsPerFilePath, jsonObject)

        val lastLocationPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_LOCATION_CODE"
        lastLocation = JsonHelper.getJSONStringAttribute(lastLocationPath, jsonObject)

        val btEnabledDuringRecordingPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BLUETOOTH_ENABLED_DURING_REC"
        bluetoothEnabledDuringRecording =
            JsonHelper.getJSONBooleanAttribute(btEnabledDuringRecordingPath, jsonObject)

        val fileHeaderPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_FILE_HEADER"
        fileHeader = JsonHelper.getJSONStringAttribute(fileHeaderPath, jsonObject)

        val logPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_LOG_CONFIG$PATH_SEPARATOR$KEY_LOG_TO_SD_CARD"
        logToSDCardEnabled = JsonHelper.getJSONBooleanAttribute(logPath, jsonObject)

    }

    fun parseStatus(jsonObject: JSONObject) {
        val sessionIdPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_IDENTIFIER"
        sessionId = JsonHelper.getJSONStringAttribute(sessionIdPath, jsonObject)

        val hoursPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_TIME"
        val recordingHours = JsonHelper.getJSONNumberAttribute(hoursPath, jsonObject)
        recordingSeconds = TimeHelper.toSeconds(recordingHours)

        val hoursSinceBootPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_TOTAL_RECORDING_TIME"
        recHoursSinceBoot = JsonHelper.getJSONNumberAttribute(hoursSinceBootPath, jsonObject)

        val versionPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_FIRMWARE"
        version = JsonHelper.getJSONStringAttribute(versionPath, jsonObject)

        val uptimePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_UPTIME"
        uptimeHours = JsonHelper.getJSONNumberAttribute(uptimePath, jsonObject)

        val batteryLevelPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_LEVEL"
        batteryLevel = JsonHelper.getJSONNumberAttribute(batteryLevelPath, jsonObject)

        val voltagePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_VOLTAGE"
        batteryVoltage = JsonHelper.getJSONNumberAttribute(voltagePath, jsonObject)

        val batteryTypePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_TYPE"
        batteryType = JsonHelper.getJSONStringAttribute(batteryTypePath, jsonObject)

        val freeSpaceGbPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_FREE_GB"
        freeSpaceGb = JsonHelper.getJSONNumberAttribute(freeSpaceGbPath, jsonObject)

        val freeSpacePercPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_FREE_PERC"
        freeSpacePercentage = JsonHelper.getJSONNumberAttribute(freeSpacePercPath, jsonObject)

        val sdCardSizePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_SIZE"
        sdCardSizeGb = JsonHelper.getJSONNumberAttribute(sdCardSizePath, jsonObject)

        val statePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_STATE$PATH_SEPARATOR$KEY_STATE"
        val rawState = JsonHelper.getJSONStringAttribute(statePath, jsonObject).lowercase()
        deviceState = DeviceState.parse(rawState)
    }

    fun parseDeviceState(jsonObject: JSONObject) {
        val resultCode =
            JsonHelper.getJSONNumberAttribute(DeviceDriver.KEY_ECODE, jsonObject).toInt()
        val commandSucceeded = resultCode == 0
        if (commandSucceeded) {
            val raw = JsonHelper.getJSONStringAttribute(
                "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_RECORDING_STATE",
                jsonObject
            )
            deviceState = DeviceState.parse(raw)
        }
    }
}