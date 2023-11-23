package de.eloc.eloc_control_panel.ng3.activities

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityMapBinding
import de.eloc.eloc_control_panel.databinding.WindowLayoutBinding
import de.eloc.eloc_control_panel.models.ElocDeviceInfo
import de.eloc.eloc_control_panel.models.ElocMarker
import de.eloc.eloc_control_panel.ng2.models.HttpHelper
import java.util.Locale

class MapActivity : ThemableActivity() {
    companion object {
        /*
           For reference, use the info below to help set default zoom level for when a marker is tapped:
           1: World
           5: Landmass/continent
           10: City
           15: Streets
           20: Buildings
        */

        private const val MARKER_ZOOM = 12
        private const val INITIAL_ZOOM = 10 // City level

        const val EXTRA_RANGER_NAME = "ranger_name"
    }

    private var customZoom = 2
    private val unknownDevices = ArrayList<String>()
    private lateinit var binding: ActivityMapBinding
    private lateinit var rangerName: String
    private var map: GoogleMap? = null
    private var mapBounds: LatLngBounds? = null
    private var devices: ArrayList<ElocDeviceInfo>? = null
    private var clusterManager: ClusterManager<ElocMarker>? = null
    private val usedLocations = HashSet<String>()

    private val infoWindowAdapter: InfoWindowAdapter = object : InfoWindowAdapter {
        override fun getInfoContents(marker: Marker): View? {
            return null
        }

        override fun getInfoWindow(marker: Marker): View {
            // When a marker info window is shown, reset lastCustomZoom
            // so that when clusters are clicked on later, the map will not zoom too deep
            customZoom = INITIAL_ZOOM
            zoomTo(MARKER_ZOOM, marker.position)
            val windowLayoutBinding: WindowLayoutBinding =
                WindowLayoutBinding.inflate(layoutInflater)
            windowLayoutBinding.titleTextView.text = marker.title
            windowLayoutBinding.snippetTextView.text = marker.snippet
            return windowLayoutBinding.root
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        binding.mapView.visibility = View.INVISIBLE

        setContentView(binding.root)
        setListeners()
        updateUnknownDevices()
        collapseUnknownDevices()
        if (hasRangerName()) {
            val mapFragment =
                supportFragmentManager.findFragmentById(binding.mapView.id) as SupportMapFragment?
            mapFragment?.getMapAsync { googleMap: GoogleMap ->
                map = googleMap
                showMap()
            }
            HttpHelper.getInstance()
                .getElocDevicesAsync(rangerName) { info -> elocDeviceInfoReceived(info) }
        } else {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.ranger_name_required)
            ) {
                goBack()
            }
        }
    }

    private fun hasRangerName(): Boolean {
        rangerName = intent.extras?.getString(EXTRA_RANGER_NAME, "") ?: ""
        rangerName = rangerName.trim()
        return rangerName.isNotEmpty()
    }

    private fun elocDeviceInfoReceived(info: ArrayList<ElocDeviceInfo>?) {
        devices = info
        showMap()
    }

    private fun setListeners() {
        binding.unknownDevicesButton.setOnClickListener {
            if (binding.upIcon.visibility == View.VISIBLE) {
                expandUnknownDevices()
            } else {
                collapseUnknownDevices()
            }
        }
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun expandUnknownDevices() {
        binding.upIcon.visibility = View.GONE
        binding.downIcon.visibility = View.VISIBLE
        binding.unknownDevicesTextView.visibility = View.VISIBLE
    }

    private fun collapseUnknownDevices() {
        binding.upIcon.visibility = View.VISIBLE
        binding.downIcon.visibility = View.GONE
        binding.unknownDevicesTextView.visibility = View.GONE
    }

    private fun zoomTo(zoomLevel: Int, position: LatLng) {
        // Set a delay so that cluster manager does not interfere with zoom animation
        val delayMillis = 1200
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            {
                if (map == null) {
                    return@postDelayed
                }
                val cameraPosition = CameraPosition.builder()
                    .zoom(zoomLevel.toFloat())
                    .target(position)
                    .build()
                map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }, delayMillis
                .toLong()
        )
    }

    private fun showDevices() {
        mapBounds = null
        clusterManager?.clearItems()
        usedLocations.clear()
        unknownDevices.clear()
        if (devices != null) {
            for (device: ElocDeviceInfo in devices!!) {
                addMarker(device)
            }
        }
        clusterManager?.cluster()
    }

    private fun updateUnknownDevices() {
        val names = unknownDevices.toArray(arrayOf<String>())
        binding.unknownDevicesTextView.text = if (names.isEmpty()) {
            getString(R.string.none)
        } else {
            val builder = StringBuilder()
            names.sort()
            val maxIndex = names.size - 1
            for (i in 0..maxIndex) {
                builder.append(names[i])
                if (i < maxIndex) {
                    builder.append(", ")
                }
            }
            builder
        }

    }

    private fun showUnknownDevices() {
        binding.unknownDevicesPanel.visibility = View.VISIBLE
    }

    private fun addMarker(device: ElocDeviceInfo) {
        var decodedVal: OpenLocationCode.CodeArea? = null
        try {
            decodedVal = OpenLocationCode.decode(device.plusCode)
        } catch (_: IllegalArgumentException) {
        }

        // If location code is invalid, add to unknown list and skip
        if (decodedVal == null) {
            unknownDevices.add(device.name)
            showUnknownDevices()
            expandUnknownDevices()
            updateUnknownDevices()
            return
        }

        var location = LatLng(decodedVal.centerLatitude, decodedVal.centerLongitude)
        val stringifiedLocation = "" + location.latitude + ":" + location.latitude
        var offset: Double = -1.0
        if (usedLocations.contains(stringifiedLocation)) {
            val offsetLocation = LatLng(location.latitude + 0.00005, location.longitude)
            val distance = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                offsetLocation.latitude,
                offsetLocation.longitude,
                distance
            )
            location = offsetLocation
            offset = distance[0].toDouble()
        }
        mapBounds = if (mapBounds == null) {
            LatLngBounds(location, location)
        } else {
            mapBounds!!.including(location)
        }
        map?.setLatLngBoundsForCameraTarget(mapBounds)
        if (clusterManager != null) {
            var snippet = getString(
                R.string.snippet_template,
                device.time,
                getPretty(device.accuracy),
                getPretty(device.batteryVolts),
                getPretty(device.recTime)
            )
            if (offset > 0) {
                snippet += String.format(Locale.ENGLISH, "\n\n(Offset by ~ %.2fm)", offset)
            }
            val added = clusterManager?.addItem(ElocMarker(device.name, snippet, location)) ?: false
            if (added) {
                usedLocations.add(stringifiedLocation)
            }
        }
    }

    private fun getPretty(value: Double): String {
        return getString(R.string.pretty_double_template, value)
    }

    @SuppressLint("MissingPermission")
    private fun showMap() {
        if (map == null || devices == null) {
            return
        }
        synchronized(map!!) {
            runOnUiThread {
                if (map == null) {
                    return@runOnUiThread
                }
                // Note: No need for permission check here
                // because PermissionsActivity ensures that app will only launch
                // when all required permission are granted, including location.
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMapToolbarEnabled = false
                binding.mapView.visibility = View.VISIBLE
                binding.loadingLayout.visibility = View.GONE
                clusterManager = ClusterManager(this, map)
                clusterManager?.markerCollection?.setInfoWindowAdapter(infoWindowAdapter)
                clusterManager?.setOnClusterClickListener { cluster ->
                    // Changing the zoom level on each tap so that map keeps zooming in.
                    if (customZoom < INITIAL_ZOOM) {
                        customZoom = INITIAL_ZOOM
                    } else {
                        customZoom += 5
                    }
                    zoomTo(customZoom, cluster.position)
                    true
                }
                map?.setOnCameraIdleListener(clusterManager)
                showDevices()
            }
        }
    }
}