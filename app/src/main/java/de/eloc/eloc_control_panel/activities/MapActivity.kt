package de.eloc.eloc_control_panel.activities

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AppState
import de.eloc.eloc_control_panel.data.ElocDeviceInfo
import de.eloc.eloc_control_panel.data.ElocMarker
import de.eloc.eloc_control_panel.data.adapters.MapInfoAdapter
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.databinding.ActivityMapBinding
import de.eloc.eloc_control_panel.databinding.WindowLayoutBinding
import de.eloc.eloc_control_panel.dialogs.ListViewDialog
import java.util.Locale

class MapActivity : ThemableActivity() {

    private lateinit var binding: ActivityMapBinding
    private val unknownDevices = mutableSetOf<String>()
    private val usedLocations = mutableSetOf<String>()
    private var map: GoogleMap? = null
    private var mapDevices = mutableMapOf<String, ElocDeviceInfo>()
    private var deviceCache = mutableListOf<ElocDeviceInfo>()
    private var mapBounds: LatLngBounds? = null
    private var clusterManager: ClusterManager<ElocMarker>? = null

    private val infoWindowAdapter: InfoWindowAdapter = object : InfoWindowAdapter {
        override fun getInfoContents(marker: Marker): View? {
            return null
        }

        override fun getInfoWindow(marker: Marker): View {
            // When a marker info window is shown, reset lastCustomZoom
            // so that when clusters are clicked on later, the map will not zoom too deep
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
        binding.progressIndicator.visibility = View.VISIBLE
        binding.dialogBackground.visibility = View.GONE

        setContentView(binding.root)
        menuInflater.inflate(R.menu.find_elocs_menu, binding.toolbar.menu)
        setListeners()

        if (AppState.hasValidProfile) {
            val mapFragment =
                supportFragmentManager.findFragmentById(binding.mapView.id) as SupportMapFragment?
            mapFragment?.getMapAsync { googleMap: GoogleMap ->
                map = googleMap
                initializeMap()
            }

            FirestoreHelper.instance.getElocDevicesAsync { info ->
                runOnUiThread {
                    deviceCache.add(info)
                    if (map != null) {
                        showMarkers()
                    }
                }
            }
        } else {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.ranger_name_required)
            ) {
                goBack()
            }
        }
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.mnu_unknown_location -> showElocsWithUnknownLocation()
                R.id.mnu_list_view -> showElocList()
            }
            true
        }
    }

    private fun zoomIn(position: LatLng) {
        // Set a delay so that cluster manager does not interfere with zoom animation
        val delayMillis = 800
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            {
                if (map == null) {
                    return@postDelayed
                }

                /*
                For reference, use the info below to help set default zoom level for when a marker is tapped:
                1: World
                5: Landmass/continent
                10: City
                15: Streets
                20: Buildings
                */

                val cameraPosition = CameraPosition.builder()
                    .zoom(map!!.cameraPosition.zoom + 3)
                    .target(position)
                    .build()
                map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }, delayMillis
                .toLong()
        )
    }

    private fun initializeMap() {
        synchronized(map!!) {
            runOnUiThread {
                if (map == null) {
                    return@runOnUiThread
                }

                // Note: No need for permission check here
                // because PermissionsActivity ensures that app will only launch
                // when all required permission are granted, including location.
                @SuppressLint("MissingPermission")
                map?.isMyLocationEnabled = true
                binding.mapView.visibility = View.VISIBLE
                binding.progressIndicator.visibility = View.GONE
                clusterManager = ClusterManager(this, map)

                clusterManager?.markerCollection?.setInfoWindowAdapter(infoWindowAdapter)
                clusterManager?.setOnClusterClickListener { cluster ->
                    // Changing the zoom level on each tap so that map keeps zooming in.
                    zoomIn(cluster.position)
                    true
                }
                map?.setOnCameraIdleListener(clusterManager)
                showMarkers()
            }
        }
    }


    private fun showMarkers() {
        var infoAdded = false
        while (deviceCache.isNotEmpty()) {
            val newInfo = deviceCache.removeAt(0)
            // Add to map (replace older)
            val key = newInfo.name
            var add = true
            if (mapDevices.containsKey(key)) {
                val oldInfo = mapDevices[key]
                if (oldInfo != null) {
                    if (oldInfo.time.compareTo(newInfo.time) >= 1) {
                        add = false
                    }
                }
            }

            if (add) {
                infoAdded = true
                mapDevices[key] = newInfo
            }
        }

        if (infoAdded) {
            mapBounds = null
            clusterManager?.clearItems()
            usedLocations.clear()
            unknownDevices.clear()
            for (info in mapDevices) {
                addMarker(info.value)
            }
            clusterManager?.cluster()
        }
    }

    private fun getPretty(value: Double): String {
        return getString(R.string.pretty_double_template, value)
    }

    private fun addMarker(device: ElocDeviceInfo) {
        val loc = device.location
        if (loc == null) {
            unknownDevices.add(device.name)
        } else {
            var location = loc
            var stringifiedLocation = LocationHelper.prettifyLocation(location)
            var offset: Double = -1.0
            while (usedLocations.contains(stringifiedLocation)) {
                val offsetLocation =
                    LatLng(location!!.latitude + 0.00005, location.longitude)
                val distance = FloatArray(1)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    offsetLocation.latitude,
                    offsetLocation.longitude,
                    distance
                )
                location = offsetLocation
                stringifiedLocation = LocationHelper.prettifyLocation(location)
                offset = distance[0].toDouble()
            }
            mapBounds = if (mapBounds == null) {
                LatLngBounds(location!!, location)
            } else {
                mapBounds!!.including(location!!)
            }

            // Move camera to center of map bounds, but do not set bounds on map/camera
            //map?.setLatLngBoundsForCameraTarget(mapBounds)
            val centerOfMapBounds = CameraPosition.builder()
                .target(mapBounds!!.center)
                .zoom(8f)
                .build()
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(centerOfMapBounds))

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
                val added =
                    clusterManager?.addItem(ElocMarker(device.name, snippet, location)) ?: false
                if (added) {
                    usedLocations.add(stringifiedLocation)
                }
            }
        }
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) {
            resources.getDimensionPixelSize(resId)
        } else {
            (resources.displayMetrics.density * 25).toInt()
        }
    }

    private fun getActionBarSize(): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        } else {
            (resources.displayMetrics.heightPixels * 36)
        }
    }

    private fun calculateDialogHeight(): Int {
        val statusBarHeight = getStatusBarHeight()
        val screenHeight = resources.displayMetrics.heightPixels
        return screenHeight - getActionBarSize() - statusBarHeight //- statusBarHeight
    }

    private fun showElocsWithUnknownLocation() {
        val sortedItems = if (unknownDevices.isEmpty()) {
            listOf(getString(R.string.none))
        } else {
            unknownDevices.sorted()
        }
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, sortedItems)
        val height = calculateDialogHeight()
        val dialog = ListViewDialog(
            getString(R.string.elocs_with_unknown_location),
            height,
            adapter,
            { binding.dialogBackground.visibility = View.VISIBLE },
            { binding.dialogBackground.visibility = View.GONE }
        )
        dialog.show(supportFragmentManager)
    }

    private fun showElocList() {
        val adapter = if (mapDevices.isEmpty()) {
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf(getString(R.string.none))
            )
        } else {
            val sortedItems = mapDevices.values.toList().sortedWith { a, b ->
                a.name.compareTo(b.name)
            }
            MapInfoAdapter(this, R.layout.layout_map_table_item, sortedItems)
        }
        val height = calculateDialogHeight()
        val dialog = ListViewDialog(
            getString(R.string.my_elocs),
            height,
            adapter,
            { binding.dialogBackground.visibility = View.VISIBLE },
            { binding.dialogBackground.visibility = View.GONE }
        )
        dialog.show(supportFragmentManager)
    }
}
