package de.eloc.eloc_control_panel

import android.app.Application
import com.google.firebase.FirebaseApp

class App : Application() {
    private lateinit var appVersionName: String
    private lateinit var appPackageName: String

    override fun onCreate() {
        super.onCreate()
        cInstance = this
        FirebaseApp.initializeApp(this)
        val info = packageManager.getPackageInfo(packageName, 0)
        appVersionName = info.versionName
        appPackageName = info.packageName
    }

    companion object {
        private var cInstance: App? = null
          const val APP_PROTOCOL_VERSION = 1.0

        val instance: App
            get() {
                return cInstance!!
            }

        val versionName: String
            get() = cInstance!!.appVersionName

        val applicationId: String
            get() = cInstance!!.appPackageName
    }
}