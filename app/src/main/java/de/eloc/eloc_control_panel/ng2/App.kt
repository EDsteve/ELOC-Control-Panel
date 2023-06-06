package de.eloc.eloc_control_panel.ng2

import android.app.Application
import com.google.firebase.FirebaseApp

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        cInstance = this
        FirebaseApp.initializeApp(this)
    }

    companion object {
        private var cInstance: App? = null

        val instance: App
            get() {
                return cInstance!!
            }

        val version: String
            get() {
                val info = cInstance!!.applicationInfo
                return info.name
            }

        val applicationId: String
            get() {
                val info = cInstance!!.applicationInfo
                return info.packageName
            }
    }

}
