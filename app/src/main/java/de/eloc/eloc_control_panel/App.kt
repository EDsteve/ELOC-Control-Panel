package de.eloc.eloc_control_panel

import android.app.Application
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.google.firebase.FirebaseApp
import de.eloc.eloc_control_panel.interfaces.VoidCallback

class App : Application() {
    private lateinit var appVersionName: String
    private lateinit var appPackageName: String
    private var hasInternetAccess: Boolean? = true
    private var networkChangedHandlers: HashMap<String, VoidCallback> = hashMapOf()
    private var connectivityManager: ConnectivityManager? = null
    private var activeNetworkIds: HashSet<String> = hashSetOf()

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            hasInternetAccess = true
            activeNetworkIds.add(network.toString())
            onNetworkChanged()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            activeNetworkIds.remove(network.toString())
            hasInternetAccess = activeNetworkIds.isNotEmpty()
            onNetworkChanged()
        }
    }

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    override fun onCreate() {
        super.onCreate()
        cInstance = this
        FirebaseApp.initializeApp(this)
        val info = packageManager.getPackageInfo(packageName, 0)
        appVersionName = info.versionName
        appPackageName = info.packageName
        registerNetworkCallback()
    }

    fun isOnline(): Boolean? {
        return hasInternetAccess
    }

    fun onNetworkChanged() {
        for (entry in networkChangedHandlers) {
            entry.value.handler()
        }
    }

    fun addNetworkChangedHandler(id: String, handler: VoidCallback) {
        // ID must be the name of the activity.
        // Use the ID as key to avoid duplicate handlers.
        // Multiple 'adds' by the same activity will result in existing handler
        // being overwritten.
        networkChangedHandlers[id] = handler
    }

    fun removeNetworkChangedHandler(id: String) {
        networkChangedHandlers.remove(id)
    }

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            val hasWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
            val hasInternet =
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            hasInternetAccess = hasInternet || hasWifi
        } else {
            hasInternetAccess = true
        }
        onNetworkChanged()
    }

    override fun onTerminate() {
        super.onTerminate()
        for (entry in networkChangedHandlers) {
            removeNetworkChangedHandler(entry.key)
        }
        connectivityManager?.unregisterNetworkCallback(networkCallback)
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