package de.eloc.eloc_control_panel.data

import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R

enum class RecordState(val code: Int) {
    Invalid(0),
    RecordOffDetectOff(1),
    RecordOnDetectOff(2),
    RecordOnDetectOn(3),
    RecordOffDetectOn(4),
    RecordOnEvent(5);

    val isInactive get(): Boolean = ((this == Invalid) || (this == RecordOffDetectOff))

     fun getVerb(): String {
        val resId = when (this) {
            Invalid -> R.string.state_invalid
            RecordOffDetectOff -> R.string.state_verb_record_off_detect_off
            RecordOnDetectOff -> R.string.state_verb_record_on_detect_off
            RecordOnDetectOn -> R.string.state_verb_record_on_detect_on
            RecordOffDetectOn -> R.string.state_verb_record_off_detect_on
            RecordOnEvent -> R.string.state_verb_record_on_event
        }
        return App.instance.getString(resId)
    }

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