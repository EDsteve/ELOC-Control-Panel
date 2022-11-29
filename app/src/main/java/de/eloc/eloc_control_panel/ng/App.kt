package de.eloc.eloc_control_panel.ng

import android.app.Application

import de.eloc.eloc_control_panel.helpers.BluetoothHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        mAppInstance = this
        BluetoothHelper.initialize()
    }

    companion object {
        private lateinit var mAppInstance: App

        fun getInstance(): App = mAppInstance
    }

}
