package de.eloc.eloc_control_panel.data

import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.KEY_BATTERY_AVG_INTERVAL_MS
import de.eloc.eloc_control_panel.driver.KEY_BATTERY_AVG_SAMPLES
import de.eloc.eloc_control_panel.driver.KEY_BATTERY_NO_BAT_MODE
import de.eloc.eloc_control_panel.driver.KEY_BATTERY_UPDATE_INTERVAL_MS
import de.eloc.eloc_control_panel.driver.KEY_BT_ENABLE_AT_START
import de.eloc.eloc_control_panel.driver.KEY_BT_ENABLE_DURING_RECORD
import de.eloc.eloc_control_panel.driver.KEY_BT_ENABLE_ON_TAPPING
import de.eloc.eloc_control_panel.driver.KEY_BT_OFF_TIMEOUT_SECONDS
import de.eloc.eloc_control_panel.driver.KEY_CPU_ENABLE_LIGHT_SLEEP
import de.eloc.eloc_control_panel.driver.KEY_CPU_MAX_FREQUENCY_MHZ
import de.eloc.eloc_control_panel.driver.KEY_CPU_MIN_FREQUENCY_MHZ
import de.eloc.eloc_control_panel.driver.KEY_GENERAL_FILE_HEADER
import de.eloc.eloc_control_panel.driver.KEY_GENERAL_NODE_NAME
import de.eloc.eloc_control_panel.driver.KEY_GENERAL_SECONDS_PER_FILE
import de.eloc.eloc_control_panel.driver.KEY_INTRUDER_ENABLED
import de.eloc.eloc_control_panel.driver.KEY_INTRUDER_THRESHOLD
import de.eloc.eloc_control_panel.driver.KEY_INTRUDER_WINDOWS_MS
import de.eloc.eloc_control_panel.driver.KEY_LOGS_FILENAME
import de.eloc.eloc_control_panel.driver.KEY_LOGS_LOG_TO_SD_CARD
import de.eloc.eloc_control_panel.driver.KEY_LOGS_MAX_FILES
import de.eloc.eloc_control_panel.driver.KEY_LOGS_MAX_FILE_SIZE
import de.eloc.eloc_control_panel.driver.KEY_LORAWAN_ENABLE
import de.eloc.eloc_control_panel.driver.KEY_LORAWAN_REGION
import de.eloc.eloc_control_panel.driver.KEY_LORAWAN_UPLINK_INTERVAL
import de.eloc.eloc_control_panel.driver.KEY_MICROPHONE_APPLL
import de.eloc.eloc_control_panel.driver.KEY_MICROPHONE_CHANNEL
import de.eloc.eloc_control_panel.driver.KEY_MICROPHONE_SAMPLE_RATE
import de.eloc.eloc_control_panel.driver.KEY_MICROPHONE_TYPE
import de.eloc.eloc_control_panel.driver.KEY_MICROPHONE_VOLUME_POWER

enum class CommandParameterType {
    String,
    Long,
    Double,
    Boolean
}

class CommandParameter {
    private val stringValue: String?
    private val longValue: Long?
    private val doubleValue: Double?
    private val booleanValue: Boolean?
    private val parameterType: CommandParameterType

    companion object {
        fun from(raw: String): CommandParameter? {
            val data = raw.trim()
            var result: CommandParameter? = null
            // First try to get bools
            if (data.lowercase() == "true") {
                result = CommandParameter(true)
            } else if (data.lowercase() == "false") {
                result = CommandParameter(false)
            } else {
                // After bools, try to get floats
                // Note: if some float "strings" dont hav a decimal point, we might not get the float
                // -- possible logic error, but let's keep the check for "."  in place for now.
                val floatValue = data.toDoubleOrNull()
                if (data.contains(".") && (floatValue != null)) {
                    result = CommandParameter(floatValue)
                } else {
                    // .. then integers
                    val intValue = data.toLongOrNull()
                    if (intValue != null) {
                        result = CommandParameter(intValue)
                    } else {
                        // .. finally strings
                        // '#' and '=' characters are not allowed. Only strings could
                        // potentially have them. Check for those characters!
                        val hasForbiddenChars = (data.contains("#") || (data.contains("=")))
                        if (!hasForbiddenChars) {
                            result = CommandParameter(data)
                        }
                    }
                }
            }
            return result
        }
    }

    constructor(value: Boolean) {
        booleanValue = value
        parameterType = CommandParameterType.Boolean

        stringValue = null
        longValue = null
        doubleValue = null
    }

    constructor(value: String) {
        stringValue = value
        parameterType = CommandParameterType.String

        longValue = null
        doubleValue = null
        booleanValue = null
    }

    constructor(value: Long) {
        longValue = value
        parameterType = CommandParameterType.Long

        stringValue = null
        doubleValue = null
        booleanValue = null
    }

    constructor(value: Double) {
        doubleValue = value
        parameterType = CommandParameterType.Double

        longValue = null
        stringValue = null
        booleanValue = null
    }

    override fun toString(): String {
        return when (parameterType) {
            CommandParameterType.String -> stringValue!!.trim()
            CommandParameterType.Long -> "${longValue!!}"
            CommandParameterType.Double -> "${doubleValue!!}"
            CommandParameterType.Boolean -> "$booleanValue"
        }
    }

    val commandParameterType get() = parameterType

}

class Command(
    val id: Long,
    val name: String,
    private val parameters: Map<String, CommandParameter>,
    val onCommandCompleted: (String) -> Unit
) {
    var completed = false

    companion object {
        private const val SEPARATOR = "#"
        private const val ID_PREFIX = "id"
        private const val COMMAND_SET_TIME = "setTime"
        private const val COMMAND_SET_CONFIG = "setConfig"
        private const val COMMAND_GET_STATUS = "getStatus"
        private const val COMMAND_GET_CONFIG = "getConfig"

        fun createSetConfigPropertyCommand(
            property: String,
            value: String,
            commandCreatedCallback: (Command) -> Unit,
            errorCallback: () -> Unit,
            completionTask: (String) -> Unit
        ) {
            val propertyValue = value.trim().ifEmpty {
                errorCallback()
                return
            }

            val rawCommand = when (property) {
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

                KEY_LORAWAN_ENABLE -> {
                    val enabled = propertyValue.lowercase().toBooleanStrictOrNull()
                    if (enabled == null) {
                        ""
                    } else {
                        """setConfig#cfg={"config":{"lorawan":{"loraEnable":$enabled}}}"""
                    }
                }

                KEY_LORAWAN_UPLINK_INTERVAL -> {
                    val interval = propertyValue.toDoubleOrNull()?.toInt()
                    if (interval == null) {
                        ""
                    } else {
                        """setConfig#cfg={"config":{"lorawan":{"upLinkIntervalS":$interval}}}"""
                    }
                }

                KEY_LORAWAN_REGION -> {
                    """setConfig#cfg={"config":{"lorawan":{"loraRegion":"$propertyValue"}}}"""
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
            }.ifEmpty {
                errorCallback()
                return
            }
            val command = from(rawCommand, completionTask)
            commandCreatedCallback(command)
        }

        fun createSetRecordModeCommand(
            state: RecordState,
            completionTask: (String) -> Unit
        ): Command? {
            val mode = when (state) {
                RecordState.RecordOffDetectOff -> "recordOff_detectOff"
                RecordState.RecordOnDetectOff -> "recordOn_detectOff"
                RecordState.RecordOffDetectOn -> "recordOff_detectOn"
                RecordState.RecordOnDetectOn -> "recordOn_detectOn"
                RecordState.RecordOnEvent -> "recordOnEvent"
                RecordState.Invalid -> ""
            }
            if (mode.isNotEmpty()) {
                return from("setRecordMode#mode=$mode", completionTask)
            }
            return null
        }

        fun createSetLocationCommand(location: GpsData, completionTask: (String) -> Unit): Command {
            val code = location.plusCode
            val accuracy = location.accuracy.toLong()
            val rawParameter =
                """{"device":{"locationCode":"$code","locationAccuracy":$accuracy}}"""
            val cfgParameter = CommandParameter(rawParameter)
            val parameters = mapOf("cfg" to cfgParameter)
            return Command(
                DeviceDriver.nextCommandId,
                COMMAND_SET_CONFIG,
                parameters,
                completionTask
            )
        }

        fun createSetTimeCommand(
            timestampInSeconds: Long,
            timezone: Int,
            completionTask: (String) -> Unit
        ): Command {
            val timeParameter = """{"seconds":$timestampInSeconds,"ms":0,"timezone":$timezone}"""
            val parameters = mapOf("time" to CommandParameter(timeParameter))
            return Command(DeviceDriver.nextCommandId, COMMAND_SET_TIME, parameters, completionTask)
        }

        fun createGetConfigCommand(completionTask: (String) -> Unit): Command {
            return Command(DeviceDriver.nextCommandId, COMMAND_GET_CONFIG, mapOf(), completionTask)
        }

        fun createGetStatusCommand(completionTask: (String) -> Unit): Command {
            return Command(DeviceDriver.nextCommandId, COMMAND_GET_STATUS, mapOf(), completionTask)
        }

        fun from(raw: String, completionTask: (String) -> Unit): Command {
            val parts = raw.split(SEPARATOR).toMutableList()

            // Get the name
            var commandName = ""
            if (parts.isNotEmpty()) {
                commandName = parts.removeAt(0).trim()
            }

            // Set the parameters
            val parameters = mutableMapOf<String, CommandParameter>()
            var id = -1L
            while (parts.isNotEmpty()) {
                val p = parts.removeAt(0)
                val rawPair = p.split("=")
                if (rawPair.size == 2) {
                    val key = rawPair[0]
                    if (key == ID_PREFIX) {
                        val idParameter = CommandParameter.from(rawPair[1])
                        if (idParameter?.commandParameterType == CommandParameterType.Long) {
                            id = idParameter.toString().toLongOrNull() ?: -1L
                        }
                    } else {
                        val value = CommandParameter.from(rawPair[1])
                        if (value != null) {
                            parameters[key] = value
                        }
                    }
                }
            }
            if (id < 0) {
                id = DeviceDriver.nextCommandId
            }
            return Command(id, commandName, parameters, completionTask)
        }
    }

    override fun toString(): String {
        val buffer = StringBuilder()
        buffer.append(name.trim())
        buffer.append(SEPARATOR)
        buffer.append("$ID_PREFIX=$id")
        for ((key, value) in parameters) {
            val strValue = value.toString().trim()
            val strKey = key.trim()
            val token = "$SEPARATOR$strKey=$strValue"
            buffer.append(token)
        }
        buffer.append("\n")
        return buffer.toString()
    }
}