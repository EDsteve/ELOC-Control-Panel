package de.eloc.eloc_control_panel.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startUpdates(onLocationChangedCallback: (GpsData) -> Unit) {
        // Stop any existing updates first
        stopUpdates()

        this.onLocationChangedCallback = onLocationChangedCallback
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val gpsData = GpsData(
                    accuracy = location.accuracy.toInt(),
                    source = GpsDataSource.Radio,
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
                Preferences.lastKnownGpsLocation = gpsData
                onLocationChangedCallback.invoke(gpsData)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Handle status changes if needed
            }

            override fun onProviderEnabled(provider: String) {
                // Handle provider enabled if needed
            }

            override fun onProviderDisabled(provider: String) {
                // Handle provider disabled if needed
            }
        }

        listener?.let {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.0F, it)
        }
    }

    fun stopUpdates() {
        listener?.let { manager.removeUpdates(it) }
        listener = null
        onLocationChangedCallback = null
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
