package de.eloc.eloc_control_panel.ng

import android.app.Application
import android.content.SharedPreferences
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import de.eloc.eloc_control_panel.helpers.BluetoothHelper

class App : Application() {
    private lateinit var mRequestQueue: RequestQueue

    override fun onCreate() {
        super.onCreate()
        mAppInstance = this
        mRequestQueue = Volley.newRequestQueue(this)
        BluetoothHelper.initialize()
    }

    companion object {
        private lateinit var mAppInstance: App

        fun getInstance(): App = mAppInstance
    }

    fun  getRequestQueue(): RequestQueue =  mRequestQueue

    fun   getSharedPrefs(): SharedPreferences {
        // TODO: I will upgrade it to the support library for compatibility with more devices.
        // TODO: Remove when migratin to kotlin is completed
        // I do not remember the package name for the support library at the moment
        // but app will still work :)
        return getSharedPreferences("label", 0)
    }
}