package de.eloc.eloc_control_panel.data

enum class CommandType {
    SetConfig,
    SetStatus,
    GetConfig,
    GetStatus,
    SetRecordMode,
    SetTime,
    Unknown;

    val isSetCommand
        get() =
            when (this) {
                SetConfig, SetStatus -> true
                else -> false
            }
}