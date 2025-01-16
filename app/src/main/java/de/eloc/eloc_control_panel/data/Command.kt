package de.eloc.eloc_control_panel.data

import de.eloc.eloc_control_panel.driver.DeviceDriver

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
            if (data.lowercase() == "true") {
                return CommandParameter(true)
            } else if (data.lowercase() == "false") {
                return CommandParameter(false)
            } else if (data.contains("\"")) {
                return CommandParameter(data.trim())
            } else if (data.contains(".")) {
                val doubleValue = data.toDoubleOrNull()
                return if (doubleValue == null) {
                    null
                } else {
                    CommandParameter(doubleValue)
                }
            } else {
                val integer = data.toLongOrNull()
                return if (integer == null) {
                    null
                } else {
                    CommandParameter(integer)
                }
            }
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
    private val parameters: Map<String, CommandParameter>
) {

    companion object {
        private const val SEPARATOR = "#"
        private const val ID_PREFIX = "id"

        fun createSetTimeCommand(timestampInSeconds: Long, timezone: Int): Command {
            val parameters = mapOf(
                "time" to CommandParameter("""{"seconds":$timestampInSeconds,"ms":0,"timezone":$timezone}""")
            )
            return Command(DeviceDriver.getNextCommandId(), "setTime", parameters)
        }

        fun from(raw: String): Command {
            val parts = raw.split(SEPARATOR).toMutableList()

            // Get the name
            var commandName = ""
            if (parts.isNotEmpty()) {
                commandName = parts.removeFirst().trim()
            }

            // Set the parameters
            val parameters = mutableMapOf<String, CommandParameter>()
            var id = -1L
            while (parts.isNotEmpty()) {
                val p = parts.removeFirst()
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
            return Command(id, commandName, parameters)
        }
    }

    override fun toString(): String {

        val buffer = StringBuilder()
        buffer.append(name.trim())
        buffer.append(SEPARATOR)
        buffer.append("$ID_PREFIX=$id")
        for ((key, value) in parameters) {
            val token = "$SEPARATOR${key.trim()}=${value.toString().trim()}"
            buffer.append(token)
        }
        buffer.append("\n")
        return buffer.toString()
    }

}