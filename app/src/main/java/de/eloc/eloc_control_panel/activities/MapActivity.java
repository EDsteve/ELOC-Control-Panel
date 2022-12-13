package de.eloc.eloc_control_panel.activities;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.maps.android.clustering.ClusterManager;
import com.google.openlocationcode.OpenLocationCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Executors;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.adapters.RangerFilterAdapter;
import de.eloc.eloc_control_panel.databinding.ActivityMapBinding;
import de.eloc.eloc_control_panel.databinding.LayoutRangerFilterBinding;
import de.eloc.eloc_control_panel.databinding.WindowLayoutBinding;
import de.eloc.eloc_control_panel.helpers.HttpHelper;
import de.eloc.eloc_control_panel.models.ElocDeviceInfo;
import de.eloc.eloc_control_panel.models.ElocMarker;

public class MapActivity extends AppCompatActivity {
    private ActivityMapBinding binding;
    private GoogleMap map = null;
    private String[] allRangers = new String[]{};
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
            map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().zoom(15).target(marker.getPosition()).build()));
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(binding.mapView.getId());
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                map = googleMap;
                showMap();
            });
        }
        HttpHelper.getElocDevicesAsync();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_filter) {
            showFilter();
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

    private void getElocDevicessdfsdf() {
        Executors.newSingleThreadScheduledExecutor().execute(() -> {
            // Not
            devices = HttpHelper.getElocDevicesAsync();
            HashSet<String> rangerSet = new HashSet<>();
            for (ElocDeviceInfo device : devices) {
                rangerSet.add(device.ranger);
            }
            allRangers = rangerSet.toArray(new String[]{});
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Arrays.sort(allRangers, Comparator.comparing(String::toLowerCase));
            } else {
                Arrays.sort(allRangers, (a, b) -> a.toLowerCase().compareTo(b.toLowerCase()));
            }
            showMap();
        });
    }

    private void showFilter() {

        // Show the dialog in the bottom third of the device screen.
        double displayHeight = getResources().getDisplayMetrics().heightPixels;
        int dialogHeight = (int) (displayHeight / 3);

        BottomSheetDialog filterDialog = new BottomSheetDialog(this);
        filterDialog.setTitle(getString(R.string.filter_by_ranger));

        LayoutRangerFilterBinding rangerFilterBinding = LayoutRangerFilterBinding.inflate(getLayoutInflater());
        rangerFilterBinding.backButton.setOnClickListener(v -> filterDialog.dismiss());
        RangerFilterAdapter adapter = new RangerFilterAdapter(allRangers);
        rangerFilterBinding.rangerRecyclerView.setAdapter(adapter);
        rangerFilterBinding.rangerRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) rangerFilterBinding.rangerRecyclerView.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = dialogHeight;
            rangerFilterBinding.rangerRecyclerView.setLayoutParams(layoutParams);
        }
        filterDialog.setContentView(rangerFilterBinding.getRoot());
        filterDialog.setOnDismissListener(dialog -> showDevices());
        filterDialog.setOnCancelListener(dialog -> showDevices());
        filterDialog.show();
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
        ArrayList<ElocDeviceInfo> filteredList = applyFilter();
        for (ElocDeviceInfo device : filteredList) {
            addMarker(device);
        }
        clusterManager.cluster();
    }

    ArrayList<ElocDeviceInfo> applyFilter() {
        ArrayList<ElocDeviceInfo> filteredDevices = new ArrayList<>();
        HashSet<String> filter = RangerFilterAdapter.getFilter();
        if ((filter == null) || filter.isEmpty()) {
            return devices;
        }
        for (ElocDeviceInfo device : devices) {
            if (filter.contains(device.ranger)) {
                filteredDevices.add(device);
            }
        }
        return filteredDevices;
    }

    private void addMarker(ElocDeviceInfo device) {
        OpenLocationCode.CodeArea decodedVal = OpenLocationCode.decode(device.plusCode);
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
                    "GPS Accuracy: " + getPretty(2, device.accuracy) + "\n" +
                    "\n" +
                    "Battery volts: " + getPretty(2, device.batteryVolts) + "\n" +
                    "\n" +
                    "Ranger: " + device.ranger + "\n" +
                    "\n" +
                    "Rec Time: " + getPretty(2, device.recTime);
            if (offset > 0) {
                snippet += String.format(Locale.ENGLISH, "\n\n(Offset by ~ %.2fm)", offset);
            }
            boolean added = clusterManager.addItem(new ElocMarker(device.name, snippet, location));
            if (added) {
                usedLocations.add(stringifiedLocation);
            }
        }
    }

    private String getPretty(int decimalPlaces, double value) {
        String formatString = "%." + decimalPlaces + "f";
        return String.format(Locale.ENGLISH, formatString, value);
    }
}