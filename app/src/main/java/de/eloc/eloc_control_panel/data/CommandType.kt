package de.eloc.eloc_control_panel.data

enum class CommandType {
    SetConfig,
    SetStatus,
    GetConfig,
    GetStatus,
    SetRecordMode,
    SetTime,
    Unknown;

    val isGetCommand get() = isGetCommand(this)
    val isSetCommand get() = isSetCommand(this)

    companion object {
        fun isGetCommand(c: CommandType): Boolean = when (c) {
            GetConfig,
            GetStatus -> true

            else -> false
        }

        fun isSetCommand(c: CommandType): Boolean = when (c) {
            SetConfig,
            SetStatus,
            SetRecordMode,
            SetTime -> true

            else -> false
        }
    }
}