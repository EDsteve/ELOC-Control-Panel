package de.eloc.eloc_control_panel.data

import com.google.openlocationcode.OpenLocationCode

enum class GpsDataSource {
    Radio,
    Cache,
    Unknown
}

data class GpsData(
    var accuracy: Int = 101,
    val source: GpsDataSource,
    private var latitude: Double? = null,
    private var longitude: Double? = null,
) {

    val plusCode: String
        get() {
            return if ((latitude != null) && (longitude != null) && (source != GpsDataSource.Unknown)) {
                OpenLocationCode(latitude!!, longitude!!).code
            } else {
                "UNKNOWN"
            }
        }

    fun serialize(): String = "$latitude$SEPARATOR$longitude$SEPARATOR$accuracy"

    companion object {
        private const val SEPARATOR = "|"

        fun deserialize(data: String): GpsData? {
            val parts = data.split(SEPARATOR)
            if (parts.size == 3) {
                val latitude = parts[0].toDoubleOrNull()
                val longitude = parts[1].toDoubleOrNull()
                val accuracy = parts[2].toIntOrNull()
                if ((latitude != null) && (longitude != null) && (accuracy != null)) {
                    return GpsData(
                        accuracy = accuracy,
                        source = GpsDataSource.Cache,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            }
            return null
        }
    }
}