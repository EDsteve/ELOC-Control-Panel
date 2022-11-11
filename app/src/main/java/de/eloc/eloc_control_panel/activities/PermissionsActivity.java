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
import de.eloc.eloc_control_panel.helpers.BluetoothHelper;

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
        binding.refreshBtn.setOnClickListener(view -> checkPermissions());
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> checkPermissions());
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBluetoothAdapter();
    }

    private void checkBluetoothAdapter() {
        if (!BluetoothHelper.isAdapterInitialized()) {
            // App will close if there is no adapter
            // This is expected behavior to make sure that
            // the BluetoothHelper class member 'adapter'
            // will always have a valid reference to an adapter.
            finish();
        }
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
        if (!BluetoothHelper.getInstance().isAdapterOn()) {
            binding.bluetoothPromptTextView.setVisibility(View.VISIBLE);
            binding.refreshBtn.setVisibility(View.VISIBLE);
            binding.status.setVisibility(View.GONE);
            binding.progressHorizontal.setVisibility(View.GONE);
            binding.permissionsBtn.setVisibility(View.GONE);
            alreadyChecking = false;
            return;
        }

        binding.bluetoothPromptTextView.setVisibility(View.GONE);
        binding.refreshBtn.setVisibility(View.GONE);
        binding.status.setVisibility(View.VISIBLE);
        binding.progressHorizontal.setVisibility(View.VISIBLE);
        binding.permissionsBtn.setVisibility(View.VISIBLE);

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
            return hasFineLocationPermission && hasCoarseLocationPermission && hasBackgroundLocationPermission();
        } else {
            return true;
        }
    }

    private boolean hasBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED);
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

                boolean showRationale = showCoarseLocationRationale || showFineLocationRationale;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    boolean showBackgroundLocationRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                    showRationale = showRationale || showBackgroundLocationRationale;
                }
                if (showRationale) {
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
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                } : new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };

        permissionLauncher.launch(permissions);
    }
}