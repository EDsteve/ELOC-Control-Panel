package de.eloc.eloc_control_panel.activities;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterManager;
import com.google.openlocationcode.OpenLocationCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.databinding.ActivityMapBinding;
import de.eloc.eloc_control_panel.databinding.WindowLayoutBinding;
import de.eloc.eloc_control_panel.helpers.HttpHelper;
import de.eloc.eloc_control_panel.models.ElocDeviceInfo;
import de.eloc.eloc_control_panel.models.ElocMarker;

public class MapActivity extends AppCompatActivity {
    /*
    For reference, use the info below to help set default zoom level for when a marker is tapped:
    1: World
    5: Landmass/continent
    10: City
    15: Streets
    20: Buildings
     */
    private static final int MARKER_ZOOM = 12;
    private static final int INITIAL_ZOOM = 10; // City level
    private int customZoom = 2;
    private ArrayList<String> unknowDevices = new ArrayList<>();
    private ActivityMapBinding binding;
    private GoogleMap map = null;
    private LatLngBounds mapBounds = null;
    private ArrayList<ElocDeviceInfo> devices = null;
    private ClusterManager<ElocMarker> clusterManager = null;
    private final HashSet<String> usedLocations = new HashSet<>();
    private final GoogleMap.InfoWindowAdapter infoWindowAdapter = new GoogleMap.InfoWindowAdapter() {

        @Nullable
        @Override
        public View getInfoContents(@NonNull Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(@NonNull Marker marker) {
            // When a marker info window is shown, reset lastCustomZoom
            // so that when clusters are clicked on later, the map will not zoom too deep
            customZoom = INITIAL_ZOOM;

            zoomTo(MARKER_ZOOM, marker.getPosition());
            WindowLayoutBinding windowLayoutBinding = WindowLayoutBinding.inflate(getLayoutInflater());
            windowLayoutBinding.titleTextView.setText(marker.getTitle());
            windowLayoutBinding.snippetTextView.setText(marker.getSnippet());
            return windowLayoutBinding.getRoot();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        binding.mapView.setVisibility(View.INVISIBLE);

        setContentView(binding.getRoot());
        setToolbar();
        setListeners();
        hideUnknownDevices();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(binding.mapView.getId());
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                map = googleMap;
                showMap();
            });
        }
        HttpHelper.getElocDevicesAsync(this::elocDeviceInfoReceived);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    private void setToolbar() {
        binding.appbar.toolbar.setTitle(R.string.find_your_elocs);
        setSupportActionBar(binding.appbar.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void elocDeviceInfoReceived(ArrayList<ElocDeviceInfo> infos) {
        devices = infos;
        showMap();
    }

    private void zoomTo(int zoomLevel, LatLng position) {
        // Set a delay so that cluster manager does not interfere with zoom animation
        int delayMillis = 1200;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                () -> map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().zoom(zoomLevel).target(position).build()))
                , delayMillis
        );

    }

    private void setListeners() {
        binding.unknownDevicesButton.setOnClickListener(v -> {
            if (binding.upIcon.getVisibility() == View.VISIBLE) {
                collapseUnknownDevices();
            } else {
                expandUnknownDevices();
            }
        });
    }

    private void expandUnknownDevices() {
        binding.upIcon.setVisibility(View.VISIBLE);
        binding.downIcon.setVisibility(View.GONE);
        binding.unknownDevicesTextView.setVisibility(View.VISIBLE);
    }

    private void collapseUnknownDevices() {
        binding.upIcon.setVisibility(View.GONE);
        binding.downIcon.setVisibility(View.VISIBLE);
        binding.unknownDevicesTextView.setVisibility(View.GONE);
    }

    private void hideUnknownDevices() {
        binding.unknownDevicesPanel.setVisibility(View.GONE);
    }

    private void showUnknownDevices() {
        binding.unknownDevicesPanel.setVisibility(View.VISIBLE);
    }

    @SuppressLint("MissingPermission")
    private void showMap() {
        if (map == null || devices == null) {
            return;
        }

        runOnUiThread(() -> {
            // Note: No need for permission check here
            // because PermissionsActivity ensures that app will only launch
            // when all required permission are granted, including location.

            map.setMyLocationEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
            binding.mapView.setVisibility(View.VISIBLE);
            binding.loadingLayout.setVisibility(View.GONE);

            clusterManager = new ClusterManager<>(this, map);
            clusterManager.getMarkerCollection().setInfoWindowAdapter(infoWindowAdapter);
            clusterManager.setOnClusterClickListener(cluster -> {
                // Changing the zoom level on each tap so that map keeps zooming in.
                if (customZoom < INITIAL_ZOOM) {
                    customZoom = INITIAL_ZOOM;
                } else {
                    customZoom += 5;
                }
                zoomTo(customZoom, cluster.getPosition());
                return true;
            });
            map.setOnCameraIdleListener(clusterManager);
            showDevices();
        });
    }


    private void showDevices() {
        mapBounds = null;
        if (clusterManager != null) {
            clusterManager.clearItems();
            usedLocations.clear();
        }
        unknowDevices.clear();
        for (ElocDeviceInfo device : devices) {
            addMarker(device);
        }
        if (clusterManager != null) {
            clusterManager.cluster();
        }
    }

    private void updateUnknownDevices() {
        StringBuilder builder = new StringBuilder();
        String[] names = unknowDevices.toArray(new String[]{});
        Arrays.sort(names);
        int maxIndex = names.length - 1;
        for (int i = 0; i <= maxIndex; i++) {
            builder.append(names[i]);
            if (i < maxIndex) {
                builder.append(", ");
            }
        }
        binding.unknownDevicesTextView.setText(builder.toString());
    }

    private void addMarker(ElocDeviceInfo device) {
        OpenLocationCode.CodeArea decodedVal = null;
        try {
            decodedVal = OpenLocationCode.decode(device.plusCode);
        } catch (IllegalArgumentException ignore) {

        }

        // If location code is invalid, add to unknown list and skip
        if (decodedVal == null) {
            unknowDevices.add(device.name);
            showUnknownDevices();
            expandUnknownDevices();
            updateUnknownDevices();
            return;
        }

        LatLng location = new LatLng(decodedVal.getCenterLatitude(), decodedVal.getCenterLongitude());
        String stringifiedLocation = location.latitude + ":" + location.longitude;
        double offset = -1;
        if (usedLocations.contains(stringifiedLocation)) {
            LatLng offsetLocation = new LatLng(location.latitude + 0.00005, location.longitude);
            float[] distance = new float[1];
            Location.distanceBetween(location.latitude, location.longitude, offsetLocation.latitude, offsetLocation.longitude, distance);
            location = offsetLocation;
            offset = distance[0];
        }
        if (mapBounds == null) {
            mapBounds = new LatLngBounds(location, location);
        } else {
            mapBounds = mapBounds.including(location);
        }
        map.setLatLngBoundsForCameraTarget(mapBounds);

        if (clusterManager != null) {
            String snippet = "Last Update: " + device.time + "\n" +
                    "\n" +
                    "GPS Accuracy: " + getPretty(device.accuracy) + "\n" +
                    "\n" +
                    "Battery volts: " + getPretty(device.batteryVolts) + "\n" +
                    "\n" +
                    "Rec Time: " + getPretty(device.recTime);
            if (offset > 0) {
                snippet += String.format(Locale.ENGLISH, "\n\n(Offset by ~ %.2fm)", offset);
            }
            boolean added = clusterManager.addItem(new ElocMarker(device.name, snippet, location));
            if (added) {
                usedLocations.add(stringifiedLocation);
            }
        }
    }

    private String getPretty(double value) {
        String formatString = "%.2f";
        return String.format(Locale.ENGLISH, formatString, value);
    }
}