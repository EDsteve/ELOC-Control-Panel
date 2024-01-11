package de.eloc.eloc_control_panel.data

enum class RecordState(val code: Int) {
    Invalid(0),
    RecordOffDetectOff(1),
    RecordOnDetectOff(2),
    RecordOnDetectOn(3),
    RecordOffDetectOn(4),
    RecordOnEvent(5);

    val isIdle get(): Boolean = ((this == Invalid) || (this == RecordOffDetectOff))

    companion object {
        fun parse(code: Int): RecordState {
            for (state in RecordState.values()) {
                if (state.code == code) {
                    return state
                }
            }
            return Invalid
        }
    }
}