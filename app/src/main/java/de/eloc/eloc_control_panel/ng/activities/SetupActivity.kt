package de.eloc.eloc_control_panel.ng.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import de.eloc.eloc_control_panel.activities.MainActivity
import de.eloc.eloc_control_panel.databinding.ActivitySetupBinding
import de.eloc.eloc_control_panel.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.ng.models.AppPreferenceManager
import java.util.Locale

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var alreadyCheckingPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        setLaunchers()
    }

    override fun onResume() {
        super.onResume()
        doChecks()
    }

    private fun doChecks() {
        binding.bluetoothOffLayout.visibility = View.INVISIBLE
        binding.manualBluetoothLayout.visibility = View.INVISIBLE
        binding.manualLocationLayout.visibility = View.INVISIBLE
        binding.noBluetoothLayout.visibility = View.INVISIBLE
        if (!BluetoothHelper.isAdapterInitialized()) {
            hideProgress()
            binding.noBluetoothLayout.visibility = View.VISIBLE
        } else {
            checkPermissions()
        }
    }

    private fun setLaunchers() {
        permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
        ) { doChecks() }
    }

    private fun setListeners() {
        binding.closeAppButton.setOnClickListener { finish() }
        binding.bluetoothPermissionsButton.setOnClickListener { openSystemAppSettings() }
        binding.locationPermissionsButton.setOnClickListener { openSystemAppSettings() }
        binding.refreshButton.setOnClickListener { doChecks() }
        binding.turnBluetoothButton.setOnClickListener { turnOnBluetooth() }
    }

    private fun needsBTPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScanPermission = (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED)
            val hasConnectPermission = (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED)
            return !(hasScanPermission && hasConnectPermission)
        }
        // Android 11 and below should automatically get bt permission.
        return false
    }

    private fun hasLocationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasFineLocationPermission = (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)

            val hasCoarseLocationPermission = (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)

            (hasFineLocationPermission && hasCoarseLocationPermission)
        } else {
            true
        }
    }

    private fun askBtPermissions() {
        hideProgress()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                    arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
            )
        }
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (needsBTPermissions()) {
                val buffer = StringBuilder()
                val showScanRationale =
                        shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)
                val showConnectRationale =
                        shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)
                if (showScanRationale) {
                    buffer.append("Scanning for bluetooth devices\n")
                }
                if (showConnectRationale) {
                    buffer.append("Connecting to bluetooth devices\n")
                }
                var message = buffer.toString().trim()
                if (message.isEmpty()) {
                    askBtPermissions()
                } else {
                    message = String.format(
                            Locale.ENGLISH,
                            "This app needs permissions for:\n\n%s\n\nDo you want to grant the permissions now?",
                            message
                    )
                    AppPreferenceManager.setPromptedBluetooth()
                    AlertDialog.Builder(this)
                            .setTitle("Permissions required")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                            .setPositiveButton("Yes, Grant permissions") { dialog, _ ->
                                run {
                                    dialog.dismiss()
                                    askBtPermissions()
                                }
                            }
                            .show()
                }
            }
        }
    }

    private fun askLocationPermissions() {
        val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        permissionLauncher.launch(permissions)
    }

    private fun checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasLocationPermissions()) {
                val showFineLocationRationale =
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                val showCoarseLocationRationale =
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

                val showRationale = showCoarseLocationRationale || showFineLocationRationale

                if (showRationale) {
                    AppPreferenceManager.setPromptedLocation()
                    val message =
                            "This app needs to use the device location for recording coordinates.\n\nDo you want to grant the permissions now?"
                    AlertDialog.Builder(this)
                            .setTitle("Permissions required")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                            .setPositiveButton("Yes, Grant permissions") { dialog, _ ->
                                run {
                                    dialog.dismiss()
                                    askLocationPermissions()
                                }
                            }
                            .show()
                } else {
                    askLocationPermissions()
                }
            }
        }
    }

    private fun checkPermissions() {
        if (alreadyCheckingPermissions) {
            return
        }
        alreadyCheckingPermissions = true
        showProgress()
        if (needsBTPermissions()) {
            if (AppPreferenceManager.getPromptedBluetooth()) {
                hideProgress()
                binding.manualBluetoothLayout.visibility = View.VISIBLE
            } else {
                checkBluetoothPermissions()
            }
            alreadyCheckingPermissions = false
            return
        } else if (!hasLocationPermissions()) {
            if (AppPreferenceManager.getPromptedLocation()) {
                hideProgress()
                binding.manualLocationLayout.visibility = View.VISIBLE
            } else {
                checkLocationPermissions()
            }
            alreadyCheckingPermissions = false
            return
        }
        alreadyCheckingPermissions = false
        if (!BluetoothHelper.getInstance().isAdapterOn) {
            hideProgress()
            binding.bluetoothOffLayout.visibility = View.VISIBLE
            return
        }

        val intent = Intent(this, MainActivity::class.java)
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
        startActivity(intent)
        finish()
    }

    private fun hideProgress() {
        binding.status.visibility = View.INVISIBLE
        binding.progressHorizontal.visibility = View.INVISIBLE
    }

    private fun showProgress() {
        binding.status.visibility = View.VISIBLE
        binding.progressHorizontal.visibility = View.VISIBLE
    }

    private fun openSystemAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri

        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun turnOnBluetooth() {
        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(intent)
    }
}