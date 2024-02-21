package de.eloc.eloc_control_panel.activities

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import de.eloc.eloc_control_panel.interfaces.VoidCallback

abstract class NetworkMonitoringActivity : ThemableActivity() {
    private var connectivityManager: ConnectivityManager? = null

    protected var networkChangedHandler: VoidCallback? = null
    protected var hasInternetAccess: Boolean? = null
        private set

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            hasInternetAccess = true
            onNetworkChanged()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            hasInternetAccess = false
            onNetworkChanged()
        }

    }

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    protected fun onNetworkChanged() {
        runOnUiThread {
            networkChangedHandler?.handler()
        }
    }
}