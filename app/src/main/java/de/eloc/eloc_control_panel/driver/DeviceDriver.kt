package de.eloc.eloc_control_panel.driver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.Channel
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.MicrophoneVolumePower
import de.eloc.eloc_control_panel.data.InfoType
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.SampleRate
import de.eloc.eloc_control_panel.data.TimePerFile
import de.eloc.eloc_control_panel.data.UploadType
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.helpers.FileSystemHelper
import de.eloc.eloc_control_panel.data.helpers.JsonHelper
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.interfaces.ConnectionStatusListener
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback
import de.eloc.eloc_control_panel.services.StatusUploadService
import org.json.JSONObject
import java.io.IOException
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors

private const val KEY_PAYLOAD = "payload"
private const val KEY_RECORDING_STATE = "recordingState"
private const val KEY_SESSION = "session"
private const val KEY_IDENTIFIER = "identifier"
private const val KEY_DEVICE = "device"
private const val KEY_FIRMWARE = "firmware"
private const val KEY_VAL = "val"

private const val KEY_BATTERY = "battery"
private const val KEY_BATTERY_TYPE = "type"
internal const val KEY_BATTERY_UPDATE_INTERVAL_MS = "updateIntervalMs"
internal const val KEY_BATTERY_AVG_SAMPLES = "avgSamples"
internal const val KEY_BATTERY_AVG_INTERVAL_MS = "avgIntervalMs"
internal const val KEY_BATTERY_NO_BAT_MODE = "noBatteryMode"

private const val KEY_MICROPHONE = "mic"
internal const val KEY_MICROPHONE_SAMPLE_RATE = "MicSampleRate"
internal const val KEY_MICROPHONE_TYPE = "MicType"
internal const val KEY_MICROPHONE_CHANNEL = "MicChannel"
internal const val KEY_MICROPHONE_APPLL = "MicUseAPLL"
internal const val KEY_MICROPHONE_VOLUME_POWER = "MicVolume2_pwr"

private const val KEY_CONFIG = "config"
private const val KEY_LOCATION_CODE = "locationCode"
private const val PATH_SEPARATOR = JsonHelper.PATH_SEPARATOR

private const val KEY_INTRUDER_CONFIG = "intruderCfg"
internal const val KEY_INTRUDER_ENABLED = "enable"
internal const val KEY_INTRUDER_THRESHOLD = "threshold"
internal const val KEY_INTRUDER_WINDOWS_MS = "windowsMs"

internal const val KEY_BT_ENABLE_DURING_RECORD = "bluetoothEnableDuringRecord"
internal const val KEY_BT_ENABLE_AT_START = "bluetoothEnableAtStart"
internal const val KEY_BT_ENABLE_ON_TAPPING = "bluetoothEnableOnTapping"
internal const val KEY_BT_OFF_TIMEOUT_SECONDS = "bluetoothOffTimeoutSeconds"

private const val KEY_LOG_CONFIG = "logConfig"
internal const val KEY_LOGS_LOG_TO_SD_CARD = "logToSdCard"
internal const val KEY_LOGS_FILENAME = "filename"
internal const val KEY_LOGS_MAX_FILES = "maxFiles"
internal const val KEY_LOGS_MAX_FILE_SIZE = "maxFileSize"

internal const val KEY_CPU_MAX_FREQUENCY_MHZ = "cpuMaxFrequencyMHZ"
internal const val KEY_CPU_MIN_FREQUENCY_MHZ = "cpuMinFrequencyMHZ"
internal const val KEY_CPU_ENABLE_LIGHT_SLEEP = "cpuEnableLightSleep"

internal const val KEY_GENERAL_NODE_NAME = "nodeName"
internal const val KEY_GENERAL_FILE_HEADER = "fileHeader"
internal const val KEY_GENERAL_SECONDS_PER_FILE = "secondsPerFile"
internal const val KEY_GENERAL_LOCATION_ACCURACY = "locationAccuracy"

private const val KEY_CMD = "cmd"
private const val KEY_ECODE = "ecode"
private const val CMD_GET_CONFIG = "getConfig"
private const val CMD_GET_STATUS = "getStatus"
private const val CMD_SET_CONFIG = "setConfig"
private const val CMD_SET_STATUS = "setStatus"
private const val CMD_SET_TIME = "setTime"
private const val CMD_SET_RECORD_MODE = "setRecordMode"

private const val KEY_DETECTION = "detection"
private const val KEY_STATE = "state"
private const val KEY_DETECTED_EVENTS = "detectedEvents"
private const val KEY_AI_MODEL = "aiModel"

private const val KEY_RECORDING_TIME = "recordingTime"
private const val KEY_B_RECORDING_TIME = "recordingTime[h]"

private const val KEY_TOTAL_RECORDING_TIME = "totalRecordingTime"
private const val KEY_B_TOTAL_RECORDING_TIME = "totalRecordingTime[h]"

private const val KEY_UPTIME = "Uptime"
private const val KEY_B_UPTIME = "Uptime[h]"

private const val KEY_SDCARD_SIZE = "SdCardSize"
private const val KEY_B_SDCARD_SIZE = "SdCardSize[GB]"

private const val KEY_SDCARD_FREE_GB = "SdCardFreeSpaceGB"
private const val KEY_B_SDCARD_FREE_GB = "SdCardFreeSpace[GB]"

private const val KEY_SDCARD_FREE_PERC = "SdCardFreeSpacePerc"
private const val KEY_B_SDCARD_FREE_PERC = "SdCardFreeSpace[%]"

private const val KEY_BATTERY_LEVEL = "SoC"
private const val KEY_B_BATTERY_LEVEL = "SoC[%]"

private const val KEY_BATTERY_VOLTAGE = "voltage"
private const val KEY_B_BATTERY_VOLTAGE = "voltage[V]"

private const val KEY_DETECTING_TIME = "detectingTime"
private const val KEY_B_DETECTING_TIME = "detectingTime[h]"

private const val KEY_BATTERY_VOLTS = "batteryVolts"
private const val KEY_GPS_ACCURACY_METERS = "gpsAccuracyMeters"
private const val KEY_LATITUDE = "latitude"
private const val KEY_LONGITUDE = "longitude"
private const val KEY_RANGER_NAME = "rangerName"
private const val KEY_REC_TIME_HOURS = "recTimeHours"
const val KEY_TIMESTAMP = "timestamp"

object DeviceDriver : Runnable {

    val microphone = Microphone()
    val intruder = Intruder()
    val logs = Logs()
    val bluetooth = BtConfig()
    val cpu = Cpu()
    val battery = Battery()
    val sdCard = SdCard()
    val general = General()
    val session = Session()

    private var cachedStatus = ""
    private var cachedConfig = ""
    private var infoType: InfoType? = null
    private var cancelConnectionMonitor = false
    private var configSaved = false
    private var statusSaved = false
    private var combinedStatusAndConfigTime: Date? = null
    private var commandLineListener: ((String) -> Unit)? = null
    private var connecting = false
    private var disconnecting = false
    private const val NEWLINE = "\n"
    private const val EOT: Byte = 4
    private var bluetoothSocket: BluetoothSocket? = null
    private val INTENT_ACTION_DISCONNECT = App.applicationId + ".Disconnect"
    private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var device: BluetoothDevice? = null
    private var bytesCache = emptyList<Byte>()
    private var statusListener: ConnectionStatusListener? = null
    private val onSetCommandCompletedListeners = mutableListOf<SetCommandCompletedCallback?>()
    private val onGetCommandCompletedListeners = mutableListOf<GetCommandCompletedCallback?>()
    private val disconnectBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // disconnect now, else would be queued until UI re-attached
            disconnect()
        }
    }

    private val connectionMonitor = Runnable {
        var elapsed = 0
        val timeout = 30
        cancelConnectionMonitor = false
        while (elapsed <= timeout) {
            if (cancelConnectionMonitor) {
                break
            }
            try {
                Thread.sleep(1000)
            } catch (_: Exception) {
            }
            elapsed++
        }
        if (!cancelConnectionMonitor) {
            // If cancelConnectionMonitor is still false and the while loop above exited
            // it means a connection was never made - Force a disconnection.
            disconnect()
        }
    }

    val name
        get() : String {
            if (general.nodeName.isNotEmpty()) {
                return general.nodeName
            }

            var buffer = try {
                device?.name?.trim() ?: ""
            } catch (_: SecurityException) {
                ""
            }
            if (buffer.isEmpty()) {
                buffer = device?.address?.trim() ?: ""
            }
            if (buffer.isEmpty()) {
                buffer = "<invalid device>"
            }
            return buffer
        }

    private var connectionStatus = ConnectionStatus.Inactive
        set(value) {
            if (field != value) {
                field = value
                statusListener?.onStatusChanged(field)
            }
        }

    fun disconnect() {
        disconnecting = true
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {
        }
        bluetoothSocket = null
        try {
            App.instance.unregisterReceiver(disconnectBroadcastReceiver)
        } catch (_: Exception) {
        }
        connectionStatus = ConnectionStatus.Inactive
        disconnecting = false
    }

    fun addOnSetCommandCompletedListener(listener: SetCommandCompletedCallback?) {
        if ((listener != null) && (!onSetCommandCompletedListeners.contains(listener))) {
            onSetCommandCompletedListeners.add(listener)
        }
    }

    fun removeOnSetCommandCompletedListener(listener: SetCommandCompletedCallback?) {
        onSetCommandCompletedListeners.remove(listener)
    }

    fun addOnGetCommandCompletedListener(listener: GetCommandCompletedCallback?) {
        if ((listener != null) && (!onGetCommandCompletedListeners.contains(listener))) {
            onGetCommandCompletedListeners.add(listener)
        }
    }

    fun removeOnGetCommandCompletedListener(listener: GetCommandCompletedCallback?) {
        onGetCommandCompletedListeners.remove(listener)
    }

    private fun write(command: String) {
        try {
            if (bluetoothSocket?.isConnected == true) {
                val sanitizedCommand = command.trim() + NEWLINE
                val data = sanitizedCommand.encodeToByteArray()
                bluetoothSocket?.outputStream?.write(data)
            } else {
                throw IOException("ELOC device not connected!")
            }
        } catch (e: IOException) {
            disconnect()
        }
    }

    fun sendCustomCommand(command: String) {
        write(command)
    }

    fun setCommandLineListener(listener: (String) -> Unit) {
        commandLineListener = listener
    }

    fun clearCommandLineListener() {
        commandLineListener = null
    }

    fun syncTime(timestampInSeconds: Long, timezone: Int) {
        val command =
            """setTime#time={"seconds":$timestampInSeconds, "ms":0 ,"timezone" : $timezone}"""
        write(command)
    }

    private fun setLocation(code: String, accuracy: Int) {
        val command =
            """setConfig#cfg={"device":{"locationCode":"$code","locationAccuracy":$accuracy}}"""
        write(command)
    }

    fun setProperty(property: String, value: String): Boolean {
        val propertyValue = value.trim().ifEmpty {
            return false
        }

        val command = when (property) {
            KEY_GENERAL_NODE_NAME -> """setConfig#cfg={"device":{"nodeName":"$propertyValue"}}"""
            KEY_GENERAL_FILE_HEADER -> """setConfig#cfg={"device":{"fileHeader":"$propertyValue"}}"""
            KEY_GENERAL_SECONDS_PER_FILE -> {
                val secs = propertyValue.toDoubleOrNull()?.toInt()
                if (secs == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"secondsPerFile":$secs}}"""
                }
            }

            KEY_MICROPHONE_TYPE -> """setConfig#cfg={"mic":{"MicType":"$propertyValue"}}"""
            KEY_MICROPHONE_VOLUME_POWER -> {
                val newPower = MicrophoneVolumePower.parse(propertyValue)
                if (newPower == null) {
                    ""
                } else {
                    """setConfig#cfg={"mic":{"MicVolume2_pwr":${newPower.rawValue.toInt()}}}"""
                }
            }

            KEY_MICROPHONE_CHANNEL -> {
                val newChannel = Channel.parse(propertyValue)
                if (newChannel == Channel.Unknown) {
                    ""
                } else {
                    """setConfig#cfg={"mic":{"MicChannel":"${newChannel.value}"}}"""
                }
            }

            KEY_MICROPHONE_SAMPLE_RATE -> {
                val rawRate = propertyValue.toDoubleOrNull() ?: 0.0
                val newRate = SampleRate.parse(rawRate)
                if (newRate == SampleRate.Unknown) {
                    ""
                } else {
                    """setConfig#cfg={"mic":{"MicSampleRate":${newRate.code}}}"""
                }
            }

            KEY_MICROPHONE_APPLL -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    """setConfig#cfg={"mic":{"MicUseAPLL":$enabled}}"""
                }
            }

            KEY_INTRUDER_ENABLED -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"intruderCfg":{"enable":$enabled}}}"""
                }
            }

            KEY_INTRUDER_THRESHOLD -> {
                val threshold = propertyValue.toDoubleOrNull()?.toInt()
                if (threshold == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"intruderCfg":{"threshold":$threshold}}}"""
                }
            }

            KEY_INTRUDER_WINDOWS_MS -> {
                val windowsMs = propertyValue.toDoubleOrNull()?.toInt()
                if (windowsMs == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"intruderCfg":{"windowsMs":$windowsMs}}}"""
                }
            }

            KEY_LOGS_LOG_TO_SD_CARD -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"logConfig":{"logToSdCard":$enabled}}}"""
                }
            }

            KEY_LOGS_FILENAME -> """setConfig#cfg={"config":{"logConfig":{"filename":"$propertyValue"}}}"""

            KEY_LOGS_MAX_FILES -> {
                val maxFiles = propertyValue.toDoubleOrNull()?.toInt()
                if (maxFiles == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"logConfig":{"maxFiles":$maxFiles}}}"""
                }
            }

            KEY_LOGS_MAX_FILE_SIZE -> {
                val maxFileSize = propertyValue.toDoubleOrNull()?.toInt()
                if (maxFileSize == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"logConfig":{"maxFileSize":$maxFileSize}}}"""
                }
            }

            KEY_BT_OFF_TIMEOUT_SECONDS -> {
                val timeout = propertyValue.toDoubleOrNull()?.toInt()
                if (timeout == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"bluetoothOffTimeoutSeconds":$timeout}}"""
                }
            }

            KEY_BT_ENABLE_AT_START -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"bluetoothEnableAtStart":$enable}}"""
                }
            }

            KEY_BT_ENABLE_ON_TAPPING -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"bluetoothEnableOnTapping":$enable}}"""
                }
            }

            KEY_BT_ENABLE_DURING_RECORD -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"bluetoothEnableDuringRecord":$enable}}"""
                }
            }

            KEY_CPU_MIN_FREQUENCY_MHZ -> {
                val min = propertyValue.toDoubleOrNull()?.toInt()
                if (min == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"cpuMinFrequencyMHZ":$min}}"""
                }
            }

            KEY_CPU_MAX_FREQUENCY_MHZ -> {
                val max = propertyValue.toDoubleOrNull()?.toInt()
                if (max == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"cpuMaxFrequencyMHZ":$max}}"""
                }
            }

            KEY_CPU_ENABLE_LIGHT_SLEEP -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"cpuEnableLightSleep":$enable}}"""
                }
            }

            KEY_BATTERY_AVG_INTERVAL_MS -> {
                val secs = propertyValue.toDoubleOrNull()
                if (secs == null) {
                    ""
                } else {
                    val intervalMillis = (secs * 1000).toInt()
                    """setConfig#cfg={"config":{"battery":{"avgIntervalMs":$intervalMillis}}}"""
                }
            }

            KEY_BATTERY_AVG_SAMPLES -> {
                val samples = propertyValue.toDoubleOrNull()?.toInt()
                if (samples == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"battery":{"avgSamples":$samples}}}"""
                }
            }

            KEY_BATTERY_UPDATE_INTERVAL_MS -> {
                val secs = propertyValue.toDoubleOrNull()
                if (secs == null) {
                    ""
                } else {
                    val intervalMillis = (secs * 1000).toInt()
                    """setConfig#cfg={"config":{"battery":{"updateIntervalMs":$intervalMillis}}}"""
                }
            }

            KEY_BATTERY_NO_BAT_MODE -> {
                val noBatteryMode = propertyValue.lowercase().toBooleanStrictOrNull()
                if (noBatteryMode == null) {
                    ""
                } else {
                    """setConfig#cfg={"config":{"battery":{"noBatteryMode":$noBatteryMode}}}"""
                }
            }

            else -> ""
        }.ifEmpty { return false }
        write(command)
        return true
    }

    private fun onConnect() {
        cancelConnectionMonitor = true
    }

    fun registerConnectionStatusListener(listener: ConnectionStatusListener) {
        statusListener = listener
    }

    fun clearConnectionStatusListener() {
        statusListener = null
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String) {
        connecting = true
        var hadError = false
        try {
            disconnect()
            connectionStatus = ConnectionStatus.Pending
            device = BluetoothHelper.getDevice(deviceAddress)

            // Connection success and most connection errors are returned asynchronously to listener
            try {
                val filter = IntentFilter(INTENT_ACTION_DISCONNECT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    App.instance.registerReceiver(
                        disconnectBroadcastReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    App.instance.registerReceiver(
                        disconnectBroadcastReceiver,
                        filter,
                    )
                }
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP)
                BluetoothHelper.stopScan {
                    doConnect()
                }
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
            hadError = true
        } finally {
            connectionStatus =
                if (bluetoothSocket?.isConnected == true) {
                    ConnectionStatus.Active
                } else if (hadError) {
                    ConnectionStatus.Inactive
                } else {
                    ConnectionStatus.Pending
                }
            connecting = false
            BluetoothHelper.scanningSpecificEloc = false
        }
    }

    private fun doConnect() {
        try {
            Executors.newSingleThreadExecutor().execute(connectionMonitor)
            bluetoothSocket?.connect()
        } catch (_: SecurityException) {
            disconnect()
            return
        }

        connectionStatus =
            if (bluetoothSocket?.isConnected == true) {
                ConnectionStatus.Active
            } else {
                ConnectionStatus.Inactive
            }
        if (connectionStatus == ConnectionStatus.Active) {
            onConnect()
            Executors.newSingleThreadExecutor().submit(this)
        } else {
            return
        }
    }

    private fun interceptCompletedSetCommand(json: String): Boolean {
        var intercepted = false
        try {
            val root = JSONObject(json)
            val errorCode = JsonHelper.getJSONNumberAttribute(KEY_ECODE, root, -1.0).toInt()
            val commandType = when (JsonHelper.getJSONStringAttribute(KEY_CMD, root).lowercase()) {
                CMD_SET_STATUS.lowercase() -> CommandType.SetStatus
                CMD_SET_CONFIG.lowercase() -> CommandType.SetConfig
                CMD_SET_RECORD_MODE.lowercase() -> CommandType.SetRecordMode
                CMD_SET_TIME.lowercase() -> CommandType.SetTime
                else -> CommandType.Unknown
            }
            intercepted = (commandType != CommandType.Unknown)
            if (intercepted) {
                if (commandType == CommandType.SetRecordMode) {
                    parseDeviceState(root)
                }
                val success = (errorCode == 0)
                onSetCommandCompletedListeners.forEach {
                    it?.handler(success, commandType)
                }
            }
        } catch (_: Exception) {

        }
        return intercepted
    }

    private fun interceptCompletedGetCommand(json: String): Boolean {
        var intercepted = false
        try {
            val root = JSONObject(json)
            val commandType = when (JsonHelper.getJSONStringAttribute(KEY_CMD, root).lowercase()) {
                CMD_GET_STATUS.lowercase() -> CommandType.GetStatus
                CMD_GET_CONFIG.lowercase() -> CommandType.GetConfig
                else -> CommandType.Unknown
            }
            intercepted = (commandType != CommandType.Unknown)
            if (intercepted) {
                when (commandType) {
                    CommandType.GetConfig -> {
                        parseConfig(root)
                        if (!configSaved) {
                            cachedConfig = json
                            saveLocal()
                        }
                    }

                    CommandType.GetStatus -> {
                        parseStatus(root)
                        if (!statusSaved) {
                            cachedStatus = json
                            saveLocal()
                        }
                    }

                    else -> {}
                }

                onGetCommandCompletedListeners.forEach {
                    it?.handler(commandType)
                }
            }
        } catch (_: Exception) {

        }
        return intercepted
    }

    private fun saveLocal() {
        val now = Date(System.currentTimeMillis())

        when (infoType) {
            InfoType.Status -> {
                if (!statusSaved && cachedStatus.isNotEmpty()) {
                    statusSaved =
                        FileSystemHelper.saveDataFile(
                            cachedStatus,
                            UploadType.Status,
                            now,
                        )
                    if (statusSaved) {
                        cachedStatus = ""
                        StatusUploadService.start(App.instance)
                    }
                }
            }

            InfoType.StatusWithConfig -> {
                if (combinedStatusAndConfigTime == null) {
                    combinedStatusAndConfigTime = now
                }
                if (!statusSaved && cachedStatus.isNotEmpty()) {
                    statusSaved = FileSystemHelper.saveDataFile(
                        cachedStatus,
                        UploadType.Status,
                        combinedStatusAndConfigTime!!,
                    )
                }
                if (!configSaved && cachedConfig.isNotEmpty()) {
                    configSaved = FileSystemHelper.saveDataFile(
                        cachedConfig,
                        UploadType.Config,
                        combinedStatusAndConfigTime!!,
                    )
                }

                if (statusSaved && configSaved) {
                    // Save map data before clearing cached data.
                    // Only save map data if there is a valid location
                    val location = LocationHelper.decodePlusCode(general.lastLocation)
                    if (location != null) {
                        val batteryVolts = battery.voltage
                        val gpsAccuracy = general.locationAccuracy
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val rangerName = Preferences.rangerName
                        val recTime = general.recHoursSinceBoot
                        val timestamp = System.currentTimeMillis()
                        var locationData = """{
                            "$KEY_BATTERY_VOLTS": $batteryVolts,
                            "$KEY_GPS_ACCURACY_METERS": $gpsAccuracy,
                            "$KEY_LATITUDE": $latitude,
                            "$KEY_LONGITUDE": $longitude,
                            "$KEY_RANGER_NAME": "$rangerName",
                            "$KEY_REC_TIME_HOURS": $recTime,
                            "$KEY_TIMESTAMP": $timestamp
                            }"""
                        val doubleSpace = "  "
                        val singleSpace = " "
                        while (locationData.contains(doubleSpace)) {
                            locationData = locationData.replace(doubleSpace, singleSpace)
                        }
                        FileSystemHelper.saveDataFile(
                            locationData,
                            UploadType.Map,
                            combinedStatusAndConfigTime!!
                        )
                    }

                    // Clear cached data and try uploading files
                    cachedStatus = ""
                    cachedConfig = ""
                    infoType = null
                    combinedStatusAndConfigTime = null
                    StatusUploadService.start(App.instance)
                }
            }

            null -> {}
        }
    }

    private fun sanitize(s: String): String {
        return s.replace(KEY_B_DETECTING_TIME, KEY_DETECTING_TIME)
            .replace(KEY_B_BATTERY_VOLTAGE, KEY_BATTERY_VOLTAGE)
            .replace(KEY_B_BATTERY_LEVEL, KEY_BATTERY_LEVEL)
            .replace(KEY_B_SDCARD_FREE_PERC, KEY_SDCARD_FREE_PERC)
            .replace(KEY_B_SDCARD_FREE_GB, KEY_SDCARD_FREE_GB)
            .replace(KEY_B_SDCARD_SIZE, KEY_SDCARD_SIZE)
            .replace(KEY_B_UPTIME, KEY_UPTIME)
            .replace(KEY_B_TOTAL_RECORDING_TIME, KEY_TOTAL_RECORDING_TIME)
            .replace(KEY_B_RECORDING_TIME, KEY_RECORDING_TIME)
    }

    override fun run() {
        try {
            val buffer = ByteArray(1024)
            while (bluetoothSocket?.isConnected == true) {
                try {
                    val byteCount = bluetoothSocket?.inputStream?.read(buffer) ?: 0
                    val data = buffer.copyOf(byteCount)
                    bytesCache += data.toList()
                    while (true) {
                        val offset = bytesCache.indexOf(EOT)
                        if (offset < 0) {
                            break
                        }
                        val jsonBytes = bytesCache.slice(0 until offset)
                        val startIndex = offset + 1
                        val endIndex = bytesCache.size - 1
                        bytesCache = bytesCache.slice(startIndex..endIndex)
                        val jsonByteArray = jsonBytes.toByteArray()
                        val json = sanitize(String(jsonByteArray))
                        commandLineListener?.invoke(json)
                        val intercepted = interceptCompletedSetCommand(json)
                        if (!intercepted) {
                            interceptCompletedGetCommand(json)
                        }
                    }
                } catch (e: Exception) {
                    if (connecting || disconnecting) {
                        // Ignore and don't report the crash that happens while connecting
                        return
                    }
                    disconnect()
                }
                try {
                    Thread.sleep(500)
                } catch (_: Exception) {
                }
            }

        } catch (e: SecurityException) {
            disconnect()
            return
        }
    }

    fun getStatus() {
        infoType = InfoType.Status
        sendGetStatusCommand()
    }

    private fun sendGetStatusCommand() {
        write("getStatus")
    }

    private fun sendGetConfigCommand() {
        write("getConfig")
    }

    fun getStatusAndConfig(saveNextInfoResponse: Boolean = false) {
        if (saveNextInfoResponse) {
            configSaved = false
            statusSaved = false
        } else {
            configSaved = true
            statusSaved = true
        }
        infoType = InfoType.StatusWithConfig

        // Get config first - so that eloc name will be loaded as soon as possible
        sendGetConfigCommand()
        sendGetStatusCommand()
    }

    fun setRecordState(
        state: RecordState,
        locationCode: String? = null,
        locationAccuracy: Double? = null
    ) {
        if ((locationCode != null) && (locationAccuracy != null)) {
            setLocation(locationCode, locationAccuracy.toInt())
        }
        val mode = when (state) {
            RecordState.RecordOffDetectOff -> "recordOff_detectOff"
            RecordState.RecordOnDetectOff -> "recordOn_detectOff"
            RecordState.RecordOffDetectOn -> "recordOff_detectOn"
            RecordState.RecordOnDetectOn -> "recordOn_detectOn"
            RecordState.RecordOnEvent -> "recordOnEvent"
            RecordState.Invalid -> ""
        }
        if (mode.isNotEmpty()) {
            write("setRecordMode#mode=$mode")
        }
        getStatusAndConfig(true)
    }

    private fun parseConfig(jsonObject: JSONObject) {
        val generalSecondsPerFilePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_GENERAL_SECONDS_PER_FILE"
        val secondsPerFile =
            JsonHelper.getJSONNumberAttribute(generalSecondsPerFilePath, jsonObject).toInt()
        general.timePerFile = TimePerFile.parse(secondsPerFile)

        val generalFileHeaderPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_GENERAL_FILE_HEADER"
        general.fileHeader = JsonHelper.getJSONStringAttribute(generalFileHeaderPath, jsonObject)

        val generalNodeNamePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_GENERAL_NODE_NAME"
        general.nodeName = JsonHelper.getJSONStringAttribute(generalNodeNamePath, jsonObject)
        BluetoothHelper.changeName(device, general.nodeName)

        val generalLocationAccuracyPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_GENERAL_LOCATION_ACCURACY"
        general.locationAccuracy =
            JsonHelper.getJSONNumberAttribute(
                generalLocationAccuracyPath,
                jsonObject,
                defaultValue = 100.0,
            )
                .toInt()

        val sampleRatePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_SAMPLE_RATE"
        val rawSampleRate = JsonHelper.getJSONNumberAttribute(sampleRatePath, jsonObject)
        microphone.sampleRate = SampleRate.parse(rawSampleRate)

        val typePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_TYPE"
        microphone.type = JsonHelper.getJSONStringAttribute(typePath, jsonObject)

        val channelPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_CHANNEL"
        val rawChannel = JsonHelper.getJSONStringAttribute(channelPath, jsonObject)
        microphone.channel = Channel.parse(rawChannel)

        val apllPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_APPLL"
        microphone.useAPLL = JsonHelper.getJSONBooleanAttribute(apllPath, jsonObject)

        val gainPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_VOLUME_POWER"
        val volumePowerString = JsonHelper.getJSONStringAttribute(gainPath, jsonObject)
        microphone.volumePower =
            MicrophoneVolumePower.parse(volumePowerString) ?: MicrophoneVolumePower()

        val lastLocationPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_LOCATION_CODE"
        general.lastLocation = JsonHelper.getJSONStringAttribute(lastLocationPath, jsonObject)

        val intruderEnabledPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_INTRUDER_CONFIG$PATH_SEPARATOR$KEY_INTRUDER_ENABLED"
        intruder.enabled = JsonHelper.getJSONBooleanAttribute(intruderEnabledPath, jsonObject)

        val intruderThresholdPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_INTRUDER_CONFIG$PATH_SEPARATOR$KEY_INTRUDER_THRESHOLD"
        intruder.threshold =
            JsonHelper.getJSONNumberAttribute(intruderThresholdPath, jsonObject).toInt()

        val intruderWindowsMsPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_INTRUDER_CONFIG$PATH_SEPARATOR$KEY_INTRUDER_WINDOWS_MS"
        intruder.windowsMs =
            JsonHelper.getJSONNumberAttribute(intruderWindowsMsPath, jsonObject).toInt()

        val logToSdCardPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_LOG_CONFIG$PATH_SEPARATOR$KEY_LOGS_LOG_TO_SD_CARD"
        logs.logToSdCard = JsonHelper.getJSONBooleanAttribute(logToSdCardPath, jsonObject)

        val logFilenamePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_LOG_CONFIG$PATH_SEPARATOR$KEY_LOGS_FILENAME"
        logs.filename = JsonHelper.getJSONStringAttribute(logFilenamePath, jsonObject)

        val logMaxFilesPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_LOG_CONFIG$PATH_SEPARATOR$KEY_LOGS_MAX_FILES"
        logs.maxFiles = JsonHelper.getJSONNumberAttribute(logMaxFilesPath, jsonObject).toInt()

        val logMaxFileSizePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_LOG_CONFIG$PATH_SEPARATOR$KEY_LOGS_MAX_FILE_SIZE"
        logs.maxFileSize = JsonHelper.getJSONNumberAttribute(logMaxFileSizePath, jsonObject).toInt()

        val btEnableAtStartPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BT_ENABLE_AT_START"
        bluetooth.enableAtStart =
            JsonHelper.getJSONBooleanAttribute(btEnableAtStartPath, jsonObject)

        val btEnableOnTappingPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BT_ENABLE_ON_TAPPING"
        bluetooth.enableOnTapping =
            JsonHelper.getJSONBooleanAttribute(btEnableOnTappingPath, jsonObject)

        val btEnableDuringRecordPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BT_ENABLE_DURING_RECORD"
        bluetooth.enableDuringRecord =
            JsonHelper.getJSONBooleanAttribute(btEnableDuringRecordPath, jsonObject)

        val btTimeoutPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BT_OFF_TIMEOUT_SECONDS"
        bluetooth.offTimeoutSeconds =
            JsonHelper.getJSONNumberAttribute(btTimeoutPath, jsonObject).toInt()

        val cpuMaxFrequencyPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_CPU_MAX_FREQUENCY_MHZ"
        cpu.maxFrequencyMHz =
            JsonHelper.getJSONNumberAttribute(cpuMaxFrequencyPath, jsonObject).toInt()

        val cpuMinFrequencyPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_CPU_MIN_FREQUENCY_MHZ"
        cpu.minFrequencyMHz =
            JsonHelper.getJSONNumberAttribute(cpuMinFrequencyPath, jsonObject).toInt()

        val cpuEnableLightSleepPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_CPU_ENABLE_LIGHT_SLEEP"
        cpu.enableLightSleep =
            JsonHelper.getJSONBooleanAttribute(cpuEnableLightSleepPath, jsonObject)

        val batteryUpdateIntervalMsPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_UPDATE_INTERVAL_MS"
        battery.updateIntervalSecs =
            JsonHelper.getJSONNumberAttribute(batteryUpdateIntervalMsPath, jsonObject) / 1000.0

        val batteryAvgSamplesPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_AVG_SAMPLES"
        battery.avgSamples =
            JsonHelper.getJSONNumberAttribute(batteryAvgSamplesPath, jsonObject).toInt()

        val batteryAvgIntervalMsPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_AVG_INTERVAL_MS"
        battery.avgIntervalSecs =
            JsonHelper.getJSONNumberAttribute(batteryAvgIntervalMsPath, jsonObject) / 1000.0

        val batteryNoBatModePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_NO_BAT_MODE"
        battery.noBatteryMode = JsonHelper.getJSONBooleanAttribute(batteryNoBatModePath, jsonObject)
    }

    private fun parseStatus(jsonObject: JSONObject) {
        val sessionIdPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_IDENTIFIER"
        session.ID = JsonHelper.getJSONStringAttribute(sessionIdPath, jsonObject)

        val hoursPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_TIME"
        val recordingHours = JsonHelper.getJSONNumberAttribute(hoursPath, jsonObject)
        session.recordingDurationSeconds = TimeHelper.toSeconds(recordingHours)

        val detectionStatePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_DETECTION$PATH_SEPARATOR$KEY_STATE"
        session.detecting = JsonHelper.getJSONBooleanAttribute(detectionStatePath, jsonObject)

        val detectionHoursPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_DETECTION$PATH_SEPARATOR$KEY_DETECTING_TIME"
        val detectionHours = JsonHelper.getJSONNumberAttribute(detectionHoursPath, jsonObject)
        session.detectingDurationSeconds = TimeHelper.toSeconds(detectionHours)

        val detectedEventsPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_DETECTION$PATH_SEPARATOR$KEY_DETECTED_EVENTS"
        session.eventsDetected =
            JsonHelper.getJSONNumberAttribute(detectedEventsPath, jsonObject).toInt()

        val aiModelPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_DETECTION$PATH_SEPARATOR$KEY_AI_MODEL"
        session.aiModel = JsonHelper.getJSONStringAttribute(aiModelPath, jsonObject)

        val hoursSinceBootPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_TOTAL_RECORDING_TIME"
        general.recHoursSinceBoot =
            JsonHelper.getJSONNumberAttribute(hoursSinceBootPath, jsonObject)

        val versionPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_FIRMWARE"
        general.version = JsonHelper.getJSONStringAttribute(versionPath, jsonObject)

        val uptimePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_UPTIME"
        general.uptimeHours = JsonHelper.getJSONNumberAttribute(uptimePath, jsonObject)

        val batteryLevelPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_LEVEL"
        battery.level = JsonHelper.getJSONNumberAttribute(batteryLevelPath, jsonObject)

        val voltagePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_VOLTAGE"
        battery.voltage = JsonHelper.getJSONNumberAttribute(voltagePath, jsonObject)

        val batteryTypePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_TYPE"
        battery.type = JsonHelper.getJSONStringAttribute(batteryTypePath, jsonObject)

        val freeSpaceGbPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_FREE_GB"
        sdCard.freeGb = JsonHelper.getJSONNumberAttribute(freeSpaceGbPath, jsonObject)

        val freeSpacePercPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_FREE_PERC"
        sdCard.freePercentage =
            JsonHelper.getJSONNumberAttribute(freeSpacePercPath, jsonObject)

        val sdCardSizePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_SIZE"
        sdCard.sizeGb = JsonHelper.getJSONNumberAttribute(sdCardSizePath, jsonObject)

        val statePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_STATE$PATH_SEPARATOR$KEY_VAL"
        val stateCode = JsonHelper.getJSONNumberAttribute(statePath, jsonObject).toInt()
        session.recordingState = RecordState.parse(stateCode)
    }

    private fun parseDeviceState(jsonObject: JSONObject) {
        val resultCode =
            JsonHelper.getJSONNumberAttribute(KEY_ECODE, jsonObject).toInt()
        val commandSucceeded = resultCode == 0
        if (commandSucceeded) {
            val code = JsonHelper.getJSONStringAttribute(
                "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_RECORDING_STATE$PATH_SEPARATOR$KEY_VAL",
                jsonObject
            ).toInt()
            session.recordingState = RecordState.parse(code)
        }
    }
}