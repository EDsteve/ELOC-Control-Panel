package de.eloc.eloc_control_panel.data

import com.google.android.gms.maps.model.LatLng

class ElocDeviceInfo(
    val location: LatLng?,
    val name: String,
    val batteryVolts: Double,
    val time: String,
    val recTime: Double,
    val accuracy: Double,
)