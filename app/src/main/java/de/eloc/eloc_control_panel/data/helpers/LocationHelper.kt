package de.eloc.eloc_control_panel.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.App

object LocationHelper {
    private var listener: LocationListener? = null
    private val manager = App.instance.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun startUpdates(locationListener: LocationListener) {
        listener = locationListener
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.0F, listener!!)
    }

    fun stopUpdates() {
        if (listener != null) {
            manager.removeUpdates(listener!!)
        }
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