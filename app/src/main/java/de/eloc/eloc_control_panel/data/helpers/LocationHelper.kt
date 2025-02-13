package de.eloc.eloc_control_panel.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.GpsData
import de.eloc.eloc_control_panel.data.GpsDataSource
import de.eloc.eloc_control_panel.data.util.Preferences

object LocationHelper {
    private val manager = App.instance.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val unknownLocation: String get() = "UNKNOWN"
    private var onLocationChangedCallback: ((GpsData) -> Unit)? = null
    private var listener: LocationListener = LocationListener { location ->
        val gpsData = GpsData(
            accuracy = location.accuracy.toInt(),
            source = GpsDataSource.Radio,
            latitude = location.latitude,
            longitude = location.longitude,
        )
        Preferences.lastKnownGpsLocation = gpsData
        onLocationChangedCallback?.invoke(gpsData)
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(onLocationChangedCallback: (GpsData) -> Unit) {
        this.onLocationChangedCallback = onLocationChangedCallback
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.0F, listener)
    }

    fun stopUpdates() {
        manager.removeUpdates(listener)
    }

    fun decodePlusCode(code: String): LatLng? {
        try {
            val decodedVal = OpenLocationCode.decode(code)
            return LatLng(decodedVal.centerLatitude, decodedVal.centerLongitude)
        } catch (_: IllegalArgumentException) {
        }
        return null
    }

    fun prettifyLocation(location: LatLng?): String =
        if (location == null) {
            "Unknown location"
        } else {
            "${location.latitude}:${location.latitude}"
        }
}