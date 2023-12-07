package de.eloc.eloc_control_panel.data

enum class DeviceState(val code: Int) {
    Dummy1(-1),
    Disabled(32),
    Continuous(33),
    Single(34),
    RecordOff (0),
    RecordOn (1),
    RecordOnEvent (2),
    RecordOnDetectOn (4),
    RecordOffDetectOn (8),
    RecordOffDetectOff (16);

    val isIdle get(): Boolean = ((this == Disabled) || (this ==RecordOffDetectOff))

    companion object {

        fun parse(raw: String): DeviceState =
            when (raw.lowercase()) {
                "disabled" -> Disabled
                "continuous" -> Continuous
                "single" -> Single
                "recordoff" -> RecordOff
                "recordon" -> RecordOn
                else -> Dummy1
            }

        fun fromCode(code: Number): DeviceState =
            when (code.toInt()) {
                RecordOff.code -> RecordOff
                RecordOn.code -> RecordOn
                RecordOnEvent.code -> RecordOnEvent
                RecordOnDetectOn.code -> RecordOnDetectOn
                RecordOffDetectOn.code -> RecordOffDetectOn
                RecordOffDetectOff.code -> RecordOffDetectOff
                Disabled.code -> Disabled
                Continuous.code -> Continuous
                Single.code -> Single
                else -> Dummy1
            }
    }
}