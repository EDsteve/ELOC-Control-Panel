package de.eloc.eloc_control_panel.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Locale;

import de.eloc.eloc_control_panel.databinding.ActivityPermissionBinding;

public class PermissionsActivity extends AppCompatActivity {
    private ActivityResultLauncher<String[]> permissionLauncher;
    private boolean alreadyChecking = false;
    private ActivityPermissionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.permissionsBtn.setOnClickListener(view -> checkPermissions());
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> checkPermissions());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        if (alreadyChecking) {
            return;
        }
        alreadyChecking = true;
        if (needsBTPermissions()) {
            checkBluetoothPermissions();
        } else {
            checkLocationPermissions();
        }

        if (hasLocationPermissions() && (!needsBTPermissions())) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            // Button for to be use when for some odd reason user repeatedly denies permission prompts
            binding.permissionsBtn.setVisibility(View.VISIBLE);
            binding.permissionsBtn.setOnClickListener(view -> checkPermissions());
        }
        alreadyChecking = false;
    }

    private boolean needsBTPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasScanPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
            boolean hasConnectPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
            return !(hasScanPermission && hasConnectPermission);
        } else {
            // Android 11 and below should automatically get bt permission.
            return false;
        }
    }

    private boolean hasLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasFineLocationPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            boolean hasCoarseLocationPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            return hasFineLocationPermission && hasCoarseLocationPermission;
        } else {
            return true;
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (needsBTPermissions()) {
                StringBuilder buffer = new StringBuilder();
                boolean showScanRationale = shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN);
                boolean showConnectRationale = shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN);
                if (showScanRationale) {
                    buffer.append("Scanning for bluetooth devices\n");
                }
                if (showConnectRationale) {
                    buffer.append("Connecting to bluetooth devices\n");
                }
                String message = buffer.toString().trim();
                if (message.isEmpty()) {
                    askBtPermissions();
                } else {
                    message = String.format(Locale.ENGLISH, "This app needs permissions for:\n\n%s\n\nDo you want to grant the permissions now?", message);
                    new AlertDialog.Builder(this)
                            .setTitle("Permissions required")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.dismiss())
                            .setPositiveButton("Yes, Grant permissions", (dialog, i) -> {
                                dialog.dismiss();
                                askBtPermissions();
                            })
                            .show();
                }

            }
        }
    }

    private void checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasLocationPermissions()) {
                boolean showFineLocationRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                boolean showCoarseLocationRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (showCoarseLocationRationale || showFineLocationRationale) {
                    String message = "This app needs to use the device location for recording coordinates.\n\nDo you want to grant the permissions now?";
                    new AlertDialog.Builder(this)
                            .setTitle("Permissions required")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.dismiss())
                            .setPositiveButton("Yes, Grant permissions", (dialog, i) -> {
                                dialog.dismiss();
                                askLocationPermissions();
                            })
                            .show();
                } else {
                    askLocationPermissions();
                }
            }
        }
    }

    private void askBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
            });
        }
    }

    private void askLocationPermissions() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        });
    }
}