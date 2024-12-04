package de.eloc.eloc_control_panel.data

enum class RssiLabel(val type: Int, val text: String) {
    None(100, "None"),
    DescriptionOnly(101, "Description only"),
    PowerOnly(102, "Power only"),
    DescriptionAndPower(103, "Description and power");

    companion object {
        fun valueOf(type: Int) = when (type) {
            PowerOnly.type -> PowerOnly
            DescriptionOnly.type -> DescriptionOnly
            DescriptionAndPower.type -> DescriptionAndPower
            else -> None
        }
    }
}

enum class RssiDescription(val text: String) {
    /*
    Strong    0 to -30 dBm	Strong signal, close to the source. Reliable connection with maximum speed and stability.
    Good    -31 to -50 dBm	Reliable connection with good performance. Minor latency or slight data rate reduction.
    Fair	-51 to -65 dBm	Usable but with noticeable performance drops. Potential for occasional stutters or delays.
    Weak	-66 to -75 dBm	Poor signal quality. Frequent dropouts and reduced data transfer speed.
    Poor	-76 to -85 dBm	Very weak signal. Severe interruptions or limited usability of the connection.
    Bad	    -86 to -90 dBm	Extremely weak signal, likely unusable. Connection may fail often or not establish at all.
    */
    Strong("Strong"),
    Good("Good"),
    Fair("Fair"),
    Weak("Weak"),
    Poor("Poor"),
    Bad("Bad")
}

class Rssi(val dBm: Int) {

    val description: RssiDescription
        get() = when (dBm) {
            in 0 downTo -30 -> RssiDescription.Strong
            in -31 downTo -50 -> RssiDescription.Good
            in -51 downTo -65 -> RssiDescription.Fair
            in -66 downTo -75 -> RssiDescription.Weak
            in -76 downTo -85 -> RssiDescription.Poor
            else -> RssiDescription.Bad
        }
}