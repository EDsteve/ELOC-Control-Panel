package de.eloc.eloc_control_panel.driver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.Channel
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.GainType
import de.eloc.eloc_control_panel.data.SampleRate
import de.eloc.eloc_control_panel.data.TimePerFile
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.helpers.JsonHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.interfaces.ConnectionStatusListener
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SocketListener
import de.eloc.eloc_control_panel.interfaces.StringCallback
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

private const val KEY_PAYLOAD = "payload"
private const val KEY_RECORDING_STATE = "recordingState"
private const val KEY_SESSION = "session"
private const val KEY_IDENTIFIER = "identifier"
private const val KEY_DEVICE = "device"
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
private const val KEY_VAL = "val"

private const val KEY_MICROPHONE = "mic"
private const val KEY_MICROPHONE_SAMPLE_RATE = "MicSampleRate"
private const val KEY_MICROPHONE_TYPE = "MicType"
private const val KEY_MICROPHONE_CHANNEL = "MicChannel"
private const val KEY_MICROPHONE_APPLL = "MicUseAPLL"
private const val KEY_MICROPHONE_TIMING_FIX = "MicUseTimingFix"
private const val KEY_MICROPHONE_GAIN = "MicBitShift"

private const val KEY_CONFIG = "config"
private const val KEY_LOCATION_CODE = "locationCode"
private const val PATH_SEPARATOR = JsonHelper.PATH_SEPARATOR

private const val KEY_INTRUDER_CONFIG = "intruderCfg"
private const val KEY_INTRUDER_ENABLED = "enable"
private const val KEY_INTRUDER_THRESHOLD = "threshold"
private const val KEY_INTRUDER_WINDOWS_MS = "windowsMs"

private const val KEY_BT_ENABLE_DURING_RECORD = "bluetoothEnableDuringRecord"
private const val KEY_BT_ENABLE_AT_START = "bluetoothEnableAtStart"
private const val KEY_BT_ENABLE_ON_TAPPING = "bluetoothEnableOnTapping"
private const val KEY_BT_OFF_TIMEOUT_SECONDS = "bluetoothOffTimeoutSeconds"

private const val KEY_LOG_CONFIG = "logConfig"
private const val KEY_LOGS_LOG_TO_SD_CARD = "logToSdCard"
private const val KEY_LOGS_FILENAME = "filename"
private const val KEY_LOGS_MAX_FILES = "maxFiles"
private const val KEY_LOGS_MAX_FILE_SIZE = "maxFileSize"

private const val KEY_CPU_MAX_FREQUENCY_MHZ = "cpuMaxFrequencyMHZ"
private const val KEY_CPU_MIN_FREQUENCY_MHZ = "cpuMinFrequencyMHZ"
private const val KEY_CPU_ENABLE_LIGHT_SLEEP = "cpuEnableLightSleep"

private const val KEY_GENERAL_NODE_NAME = "nodeName"
private const val KEY_GENERAL_FILE_HEADER = "fileHeader"
private const val KEY_GENERAL_SECONDS_PER_FILE = "secondsPerFile"

private const val KEY_CMD = "cmd"
private const val KEY_ECODE = "ecode"
private const val CMD_GET_CONFIG = "getConfig"
private const val CMD_GET_STATUS = "getStatus"
private const val CMD_SET_CONFIG = "setConfig"
private const val CMD_SET_STATUS = "setStatus"
private const val CMD_SET_TIME = "setTime"
private const val CMD_SET_RECORD_MODE = "setRecordMode"

class Battery {
    var level = 0.0
        internal set

    var voltage = 0.0
        internal set

    var type = ""
        internal set
}

class SdCard {
    var freeGb = 0.0
        internal set

    var freePercentage = 0.0
        internal set

    var sizeGb = 0.0
        internal set
}

class General {
    companion object {
        const val NODE_NAME = KEY_GENERAL_NODE_NAME
        const val FILE_HEADER = KEY_GENERAL_FILE_HEADER
        const val SECONDS_PER_FILE = KEY_GENERAL_SECONDS_PER_FILE
    }

    var nodeName = ""
        internal set

    var fileHeader = ""
        internal set

    var timePerFile = TimePerFile.Unknown
        internal set

    var recordingState = RecordState.Invalid
        internal set

    var recHoursSinceBoot = 0.0
        internal set

    var sessionId = ""
        internal set

    var lastLocation = ""
        internal set

    var recordingSeconds = 0.0
        internal set

    var version = ""
        internal set

    var uptimeHours = 0.0
        internal set
}

class Cpu {
    companion object {
        const val MAX_FREQUENCY = KEY_CPU_MAX_FREQUENCY_MHZ
        const val MIN_FREQUENCY = KEY_CPU_MIN_FREQUENCY_MHZ
        const val ENABLE_LIGHT_SLEEP = KEY_CPU_ENABLE_LIGHT_SLEEP
    }

    var maxFrequencyMHz = 0
        internal set

    var minFrequencyMHz = 0
        internal set

    var enableLightSleep = false
        internal set
}

class BtConfig {
    companion object {
        const val ENABLE_DURING_RECORD = KEY_BT_ENABLE_DURING_RECORD
        const val ENABLE_AT_START = KEY_BT_ENABLE_AT_START
        const val ENABLE_ON_TAPPING = KEY_BT_ENABLE_ON_TAPPING
        const val OFF_TIME_OUT_SECONDS = KEY_BT_OFF_TIMEOUT_SECONDS
    }

    var enableAtStart = false
        internal set

    var enableOnTapping = false
        internal set

    var enableDuringRecord = false
        internal set

    var offTimeoutSeconds = 0
        internal set
}

class Logs {
    companion object {
        const val LOG_TO_SD_CARD = KEY_LOGS_LOG_TO_SD_CARD
        const val FILENAME = KEY_LOGS_FILENAME
        const val MAX_FILES = KEY_LOGS_MAX_FILES
        const val MAX_FILE_SIZE = KEY_LOGS_MAX_FILE_SIZE
    }

    var logToSdCard = true
        internal set

    var filename = ""
        internal set

    var maxFiles = 0
        internal set

    var maxFileSize = 0
        internal set
}

class Intruder {
    companion object {
        const val ENABLED = KEY_INTRUDER_ENABLED
        const val THRESHOLD = KEY_INTRUDER_THRESHOLD
        const val WINDOWS_MS = KEY_INTRUDER_WINDOWS_MS
    }

    var enabled = false
        internal set

    var threshold = 0
        internal set

    var windowsMs = 0
        internal set
}

class Microphone {
    companion object {
        const val TYPE = KEY_MICROPHONE_TYPE
        const val GAIN = KEY_MICROPHONE_GAIN
        const val CHANNEL = KEY_MICROPHONE_CHANNEL
        const val SAMPLE_RATE = KEY_MICROPHONE_SAMPLE_RATE
        const val USE_APLL = KEY_MICROPHONE_APPLL
        const val USE_TIMING_FIX = KEY_MICROPHONE_TIMING_FIX
    }

    var sampleRate = SampleRate.Unknown
        internal set

    var type = ""
        internal set

    var gain = GainType.Low
        internal set

    var channel = Channel.Unknown
        internal set

    var useAPLL = false
        internal set

    var useTimingFix = false
        internal set
}

object DeviceDriver : Runnable {

    val microphone = Microphone()
    val intruder = Intruder()
    val logs = Logs()
    val bluetooth = BtConfig()
    val cpu = Cpu()
    val battery = Battery()
    val sdCard = SdCard()
    val general = General()
    private var commandLineListener: StringCallback? = null
    private var connecting = false
    private var disconnecting = false
    private const val NEWLINE = "\n"
    private const val EOT: Byte = 4
    private var bluetoothSocket: BluetoothSocket? = null
    private val INTENT_ACTION_DISCONNECT = App.applicationId + ".Disconnect"
    private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var device: BluetoothDevice? = null
    private var bytesCache = emptyList<Byte>()
    private val clients = mutableListOf<ConnectionStatusListener>()
    private val dataListenerList = mutableListOf<SocketListener>()
    private val onSetCommandCompletedListeners = mutableListOf<SetCommandCompletedCallback?>()
    private val onGetCommandCompletedListeners = mutableListOf<GetCommandCompletedCallback?>()
    private val disconnectBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onIOError(IOException("ELOC bluetooth disconnected in background!"))

            // disconnect now, else would be queued until UI re-attached
            disconnect()
        }
    }

    val name
        get() : String {
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
            field = value
            notifyClients()
        }

    private fun notifyClients() {
        for (c in clients) {
            c.onStatusChanged(connectionStatus)
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
        dataListenerList.clear()
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

    fun addCommandListener(listener: SocketListener?) {
        if ((listener != null) && (!dataListenerList.contains(listener))) {
            dataListenerList.add(listener)
        }
    }

    fun removeCommandListener(listener: SocketListener) {
        dataListenerList.remove(listener)
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
            onIOError(e)
        }
    }

    fun sendCustomCommand(command: String) {
        write(command)
    }

    fun setCommandLineListener(listener: StringCallback) {
        commandLineListener = listener
    }

    fun clearCommandLineListener() {
        commandLineListener = null
    }

    fun syncTime(timestamp: Long, difference: Long, timezone: Int) {
        val command =
            "setTime#time={\"seconds\":$timestamp, \"ms\" : $difference, \"timezone\" : $timezone}"
        write(command)
    }

    private fun setLocation(code: String, accuracy: Int) {
        val command =
            "setConfig#cfg={\"device\":{\"locationCode\":\"$code\",\"locationAccuracy\":$accuracy}}"
        write(command)
    }

    fun setProperty(property: String, value: String): Boolean {
        val propertyValue = value.trim().ifEmpty {
            return false
        }

        val command = when (property) {
            KEY_GENERAL_NODE_NAME -> "setConfig#cfg={\"device\":{\"nodeName\":\"$propertyValue\"}}"
            KEY_GENERAL_FILE_HEADER -> "setConfig#cfg={\"device\":{\"fileHeader\":\"$propertyValue\"}}"
            KEY_GENERAL_SECONDS_PER_FILE -> {
                val secs = propertyValue.toDoubleOrNull()?.toInt()
                if (secs == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"secondsPerFile\":$secs}}"
                }
            }

            KEY_MICROPHONE_TYPE -> "setConfig#cfg={\"mic\":{\"MicType\":\"$propertyValue\"}}"
            KEY_MICROPHONE_GAIN -> {
                val newGain = GainType.fromValue(propertyValue)
                if (newGain == GainType.Unknown) {
                    ""
                } else {
                    "setConfig#cfg={\"mic\":{\"MicBitShift\":${newGain.value}}}"
                }
            }

            KEY_MICROPHONE_CHANNEL -> {
                val newChannel = Channel.parse(propertyValue)
                if (newChannel == Channel.Unknown) {
                    ""
                } else {
                    "setConfig#cfg={\"mic\":{\"MicChannel\":\"${newChannel.value}\"}}"
                }
            }

            KEY_MICROPHONE_SAMPLE_RATE -> {
                val rawRate = propertyValue.toDoubleOrNull() ?: 0.0
                val newRate = SampleRate.parse(rawRate)
                if (newRate == SampleRate.Unknown) {
                    ""
                } else {
                    "setConfig#cfg={\"mic\":{\"MicSampleRate\":${newRate.code}}}"
                }
            }

            KEY_MICROPHONE_APPLL -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    "setConfig#cfg={\"mic\":{\"MicUseAPLL\":$enabled}}"
                }
            }

            KEY_MICROPHONE_TIMING_FIX -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    "setConfig#cfg={\"mic\":{\"MicUseTimingFix\":$enabled}}"
                }
            }

            KEY_INTRUDER_ENABLED -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"intruderCfg\":{\"enable\":$enabled}}}"
                }
            }

            KEY_INTRUDER_THRESHOLD -> {
                val threshold = propertyValue.toDoubleOrNull()?.toInt()
                if (threshold == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"intruderCfg\":{\"threshold\":$threshold}}}"
                }
            }

            KEY_INTRUDER_WINDOWS_MS -> {
                val windowsMs = propertyValue.toDoubleOrNull()?.toInt()
                if (windowsMs == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"intruderCfg\":{\"windowsMs\":$windowsMs}}}"
                }
            }

            KEY_LOGS_LOG_TO_SD_CARD -> {
                val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enabled == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"logConfig\":{\"logToSdCard\":$enabled}}}"
                }
            }

            KEY_LOGS_FILENAME -> "setConfig#cfg={\"config\":{\"logConfig\":{\"filename\":\"$propertyValue\"}}}"

            KEY_LOGS_MAX_FILES -> {
                val maxFiles = propertyValue.toDoubleOrNull()?.toInt()
                if (maxFiles == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"logConfig\":{\"maxFiles\":$maxFiles}}}"
                }
            }

            KEY_LOGS_MAX_FILE_SIZE -> {
                val maxFileSize = propertyValue.toDoubleOrNull()?.toInt()
                if (maxFileSize == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"logConfig\":{\"maxFileSize\":$maxFileSize}}}"
                }
            }

            KEY_BT_OFF_TIMEOUT_SECONDS -> {
                val timeout = propertyValue.toDoubleOrNull()?.toInt()
                if (timeout == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"bluetoothOffTimeoutSeconds\":$timeout}}"
                }
            }

            KEY_BT_ENABLE_AT_START -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"bluetoothEnableAtStart\":$enable}}"
                }
            }

            KEY_BT_ENABLE_ON_TAPPING -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"bluetoothEnableOnTapping\":$enable}}"
                }
            }

            KEY_BT_ENABLE_DURING_RECORD -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"bluetoothEnableDuringRecord\":$enable}}"
                }
            }

            KEY_CPU_MIN_FREQUENCY_MHZ -> {
                val min = propertyValue.toDoubleOrNull()?.toInt()
                if (min == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"cpuMinFrequencyMHZ\":$min}}"
                }
            }

            KEY_CPU_MAX_FREQUENCY_MHZ -> {
                val max = propertyValue.toDoubleOrNull()?.toInt()
                if (max == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"cpuMaxFrequencyMHZ\":$max}}"
                }
            }

            KEY_CPU_ENABLE_LIGHT_SLEEP -> {
                val enable = propertyValue.lowercase().toBooleanStrictOrNull()
                if (enable == null) {
                    ""
                } else {
                    "setConfig#cfg={\"config\":{\"cpuEnableLightSleep\":$enable}}"
                }
            }

            else -> ""
        }.ifEmpty { return false }
        write(command)
        return true
    }

    private fun onConnect() {
        for (l in dataListenerList) {
            l.onConnect()
        }
    }

    private fun onConnectionError(e: Exception) {
        for (l in dataListenerList) {
            l.onConnectionError(e)
        }
    }

    private fun onRead(data: ByteArray) {
        for (l in dataListenerList) {
            l.onRead(data)
        }
    }

    private fun onIOError(e: Exception) {
        for (l in dataListenerList) {
            l.onIOError(e)
        }
    }


    fun registerConnectionChangedListener(callback: ConnectionStatusListener? = null) {
        if ((callback != null) && (!clients.contains(callback))) {
            clients.add(callback)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(
        deviceAddress: String,
        dataListener: SocketListener? = null
    ) {
        connecting = true
        var hadError = false
        try {
            disconnect()
            connectionStatus = ConnectionStatus.Pending
            device = BluetoothHelper.getDevice(deviceAddress)

            // Connection success and most connection errors are returned asynchronously to listener
            try {
                App.instance.registerReceiver(
                    disconnectBroadcastReceiver, IntentFilter(INTENT_ACTION_DISCONNECT)
                )
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP)
                BluetoothHelper.stopScan {
                    doConnect()
                }
            } catch (e: Exception) {
                // todo : remove
                println(e.localizedMessage)
            }
            addCommandListener(dataListener)
        } catch (e: Exception) {
            hadError = true
        } finally {
            connectionStatus =
                if (bluetoothSocket?.isConnected == true) {
                    TimeHelper.syncBoardClock(true)
                    ConnectionStatus.Active
                } else if (hadError) {
                    ConnectionStatus.Inactive
                } else {
                    ConnectionStatus.Pending
                }
            connecting = false
        }
    }

    private fun doConnect() {
        try {
            bluetoothSocket?.connect()
        } catch (e: SecurityException) {
            onConnectionError(e)
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
                    CommandType.GetConfig -> parseConfig(root)
                    CommandType.GetStatus -> parseStatus(root)
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
                        val json = String(jsonByteArray)
                        if (commandLineListener != null) {
                            commandLineListener?.handler(json)
                        }
                        var intercepted = interceptCompletedSetCommand(json)
                        if (!intercepted) {
                            intercepted = interceptCompletedGetCommand(json)
                            if (!intercepted) {
                                onRead(jsonByteArray)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (connecting || disconnecting) {
                        // Ignore and don't report the crash that happens while connecting
                        return
                    }

                    onIOError(e)
                    disconnect()
                }
                try {
                    Thread.sleep(500)
                } catch (_: Exception) {
                }
            }

        } catch (e: SecurityException) {
            onConnectionError(e)
            disconnect()
            return
        }
    }

    fun getStatus() {
        write("getStatus")
    }

    private fun getConfig() {
        write("getConfig")
    }

    fun getDeviceInfo() {
        getStatus()
        getConfig()
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

        val timingFixPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_TIMING_FIX"
        microphone.useTimingFix = JsonHelper.getJSONBooleanAttribute(timingFixPath, jsonObject)

        val gainPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_GAIN"
        val micBitShift = JsonHelper.getJSONStringAttribute(gainPath, jsonObject)
        microphone.gain = GainType.fromValue(micBitShift)

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
    }

    private fun parseStatus(jsonObject: JSONObject) {
        val sessionIdPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_IDENTIFIER"
        general.sessionId = JsonHelper.getJSONStringAttribute(sessionIdPath, jsonObject)

        val hoursPath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_TIME"
        val recordingHours = JsonHelper.getJSONNumberAttribute(hoursPath, jsonObject)
        general.recordingSeconds = TimeHelper.toSeconds(recordingHours)

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

        val voltagePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_VOLTAGE"
        battery.voltage = JsonHelper.getJSONNumberAttribute(voltagePath, jsonObject)

        val batteryTypePath = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_TYPE"
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
        general.recordingState = RecordState.parse(stateCode)
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
            general.recordingState = RecordState.parse(code)
        }
    }
}