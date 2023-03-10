package de.eloc.eloc_control_panel.ng2

import android.app.Application
import de.eloc.eloc_control_panel.BuildConfig

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        cInstance = this
    }

    companion object {
        private var cInstance: App? = null

        val instance: App
            get() {
                return cInstance!!
            }

        val version: String
            get() = BuildConfig.VERSION_NAME
    }

}
