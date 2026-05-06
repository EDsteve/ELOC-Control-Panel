package de.eloc.eloc_control_panel

import android.app.Application
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class App : Application() {
    private lateinit var appVersionName: String
    private lateinit var appPackageName: String
    private var hasInternetAccess: Boolean? = true
    private var networkChangedHandlers: HashMap<String, () -> Unit> = hashMapOf()
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
        configureFirestorePersistence()
        val info = packageManager.getPackageInfo(packageName, 0)
        appVersionName = info.versionName ?: "<version-missing>"
        appPackageName = info.packageName
        registerNetworkCallback()
    }

    fun isOnline(): Boolean? {
        return hasInternetAccess
    }

    fun onNetworkChanged() {
        for (entry in networkChangedHandlers) {
            entry.value.invoke()
        }
    }

    fun addNetworkChangedHandler(id: String, handler: () -> Unit) {
        // ID must be the name of the activity.
        // Use the ID as key to avoid duplicate handlers.
        // Multiple 'adds' by the same activity will result in existing handler
        // being overwritten.
        networkChangedHandlers[id] = handler
    }

    private fun configureFirestorePersistence() {
        // Firestore offline persistence is on by default, but we set it explicitly
        // (a) so the behavior is documented and not relying on SDK defaults, and
        // (b) so the cache is unlimited — status/config writes queued while the
        // phone has no network are replayed when connectivity returns.
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.i(TAG, "Firestore persistence enabled (unlimited cache)")
        } catch (t: Throwable) {
            // Settings can only be applied before the first Firestore call.
            // If something already initialized Firestore, log and continue.
            Log.w(TAG, "Failed to configure Firestore persistence", t)
        }
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
            networkChangedHandlers.remove(entry.key)
        }
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    companion object {
        private const val TAG = "App"
        private var cInstance: App? = null
        const val APP_PROTOCOL_VERSION = 2.0

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