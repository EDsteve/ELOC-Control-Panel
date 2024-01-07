package de.eloc.eloc_control_panel.data

enum class DeviceState(val code: Int) {
    Dummy1(-1),
    Idle(64),
    Disabled(32),
    Continuous(33),
    Single(34),
    RecordOff(0),
    RecordOn(1),
    RecordOnEvent(2),
    RecordOnDetectOn(4),
    RecordOffDetectOn(8),
    RecordOffDetectOff(16);

    val isIdle get(): Boolean = ((this == Disabled) || (this == RecordOffDetectOff) || (this == Idle))

    companion object {

        fun parse(raw: String): DeviceState =
            when (raw.lowercase().trim()) {
                "idle" -> Idle
                "disabled" -> Disabled
                "continuous" -> Continuous
                "single" -> Single
                "recordoff" -> RecordOff
                "recordon" -> RecordOn
                "recordoff_detectoff" -> RecordOffDetectOff
                else -> {
                    Dummy1
                }
            }
    }
}