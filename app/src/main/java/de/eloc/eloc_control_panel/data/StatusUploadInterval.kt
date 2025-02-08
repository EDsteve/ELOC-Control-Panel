package de.eloc.eloc_control_panel.data

private const val MINUTE_SECS = 60

enum class StatusUploadInterval(val seconds: Int) {
    Mins15(15 * MINUTE_SECS),
    Mins30(30 * MINUTE_SECS),
    Mins60(60 * MINUTE_SECS),
    Mins120(120 * MINUTE_SECS);

    val millis get(): Long = seconds * 1000L

    override fun toString(): String =
        when (this) {
            Mins15 -> "15 mins"
            Mins30 -> "30 mins"
            Mins60 -> "1 hr"
            Mins120 -> "2 hrs"
        }

    companion object {
        fun parse(value: Int): StatusUploadInterval = when (value) {
            Mins30.seconds -> Mins30
            Mins60.seconds -> Mins60
            Mins120.seconds -> Mins120
            else -> Mins15
        }
    }
}