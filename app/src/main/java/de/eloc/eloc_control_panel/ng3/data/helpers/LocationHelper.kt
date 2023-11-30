package de.eloc.eloc_control_panel.ng3.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import de.eloc.eloc_control_panel.ng3.App

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
}