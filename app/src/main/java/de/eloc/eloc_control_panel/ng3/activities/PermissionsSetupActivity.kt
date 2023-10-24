package de.eloc.eloc_control_panel.ng3.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityPermissionsSetupBinding
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper
import de.eloc.eloc_control_panel.ng2.receivers.BluetoothWatcher
import de.eloc.eloc_control_panel.ng3.data.PreferencesHelper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PermissionsSetupActivity : ThemableActivity() {
    private var paused = false
    private lateinit var binding: ActivityPermissionsSetupBinding
    private lateinit var checkerHandle: ScheduledFuture<*>
    private val bluetoothWatcher = BluetoothWatcher(this::runChecks)
    private val bluetoothHelper = BluetoothHelper.instance
    private val preferencesHelper = PreferencesHelper.instance

    private val needsLocationPermission: Boolean
        get() {
            val fineLocationGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            return (fineLocationGranted != PackageManager.PERMISSION_GRANTED)
        }

    private val mustTurnOnLocationService: Boolean
        get() {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            if (locationManager != null) {
                val enabledProviders = locationManager.getProviders(true)
                // Passive very inaccurate and might as well be assumed to be the same as no location for this app.
                enabledProviders.remove("passive")
                return enabledProviders.isEmpty()
            }
            return true
        }

    private val mustTurnOnBluetooth: Boolean
        get() = !bluetoothHelper.isBluetoothOn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        ContextCompat.registerReceiver(
            this,
            bluetoothWatcher,
            bluetoothHelper.broadcastFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        checkerHandle = Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(
                { runOnUiThread(::runChecks) },
                0,
                500,
                TimeUnit.MILLISECONDS
            )
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        paused = false
        runChecks()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothWatcher)
        checkerHandle.cancel(true)
    }

    private fun setListeners() {
        binding.bluetoothButton.setOnClickListener { openSystemAppSettings() }
        binding.locationButton.setOnClickListener { openSystemAppSettings() }
        binding.locationStateButton.setOnClickListener { openLocationSettings() }
        binding.bluetoothStateButton.setOnClickListener { startActivity(bluetoothHelper.enablingIntent) }
    }

    private fun openSystemAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun openLocationSettings() {
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(settingsIntent)
    }

    private fun hideActions() {
        binding.title.visibility = View.INVISIBLE
        binding.locationButton.visibility = View.INVISIBLE
        binding.bluetoothButton.visibility = View.INVISIBLE
        binding.locationStateButton.visibility = View.INVISIBLE
        binding.bluetoothStateButton.visibility = View.INVISIBLE
    }

    private fun showActions(
        locationPermission: Boolean,
        locationOff: Boolean,
        bluetoothPermission: Boolean,
        bluetoothOff: Boolean
    ) {
        if (!locationPermission && !locationOff && !bluetoothPermission && !bluetoothOff) {
            return
        }
        binding.title.visibility = View.VISIBLE
        binding.locationButton.visibility = if (locationPermission) View.VISIBLE else View.GONE
        binding.bluetoothButton.visibility = if (bluetoothPermission) View.VISIBLE else View.GONE
        binding.locationStateButton.visibility = if (locationOff) View.VISIBLE else View.GONE
        binding.bluetoothStateButton.visibility = if (bluetoothOff) View.VISIBLE else View.GONE
    }

    private fun doRequestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            preferencesHelper.setLocationRequested()
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
    }

    private fun doRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            preferencesHelper.setBluetoothRequested()
            requestPermissions(BluetoothHelper.instance.bluetoothPermissions, 0)
        }
    }

    private fun askLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                val appName = getString(R.string.app_name)
                val rationale = getString(R.string.location_service_rationale, appName)
                paused = true
                val positiveCallback = {
                    doRequestLocationPermission()
                }
                val negativeCallback = {
                    showActions(
                        needsLocationPermission,
                        mustTurnOnLocationService,
                        BluetoothHelper.instance.needsBluetoothPermissions,
                        mustTurnOnBluetooth
                    )
                }
                showModalOptionAlert(
                    title = getString(R.string.permission_required),
                    message = rationale,
                    positiveButtonLabel = getString(R.string.grant_permission),
                    positiveCallback = positiveCallback,
                    negativeCallback = negativeCallback
                )
            } else {
                val alreadyRequested = preferencesHelper.getLocationRequested()
                if (alreadyRequested) {
                    paused = true
                    val positiveCallback = {
                        openSystemAppSettings()
                    }
                    val negativeCallback = {
                        showActions(
                            needsLocationPermission,
                            mustTurnOnLocationService,
                            BluetoothHelper.instance.needsBluetoothPermissions,
                            mustTurnOnBluetooth
                        )
                    }
                    showModalOptionAlert(
                        getString(R.string.user_action_required),
                        getString(R.string.manual_location),
                        getString(R.string.open_settings),
                        positiveCallback = positiveCallback,
                        negativeCallback = negativeCallback
                    )
                } else {
                    doRequestLocationPermission()
                }
            }
        }
    }

    private fun askBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            var showRationale = false
            for (permission in BluetoothHelper.instance.bluetoothPermissions) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    showRationale = true
                    break
                }
            }
            if (showRationale) {
                val appName = getString(R.string.app_name)
                val message = getString(R.string.bluetooth_rationale, appName)
                paused = true
                val positiveCallback = {
                    doRequestBluetoothPermissions()
                }
                val negativeCallback = {
                    showActions(
                        needsLocationPermission,
                        mustTurnOnLocationService,
                        BluetoothHelper.instance.needsBluetoothPermissions,
                        mustTurnOnBluetooth
                    )
                }
                showModalOptionAlert(
                    getString(R.string.permission_required),
                    message,
                    getString(R.string.grant_permission),
                    positiveCallback = positiveCallback,
                    negativeCallback = negativeCallback
                )
            } else {
                val alreadyRequested = preferencesHelper.getBluetoothRequested()
                if (alreadyRequested) {
                    paused = true
                    val positiveCallback = {
                        openSystemAppSettings()
                    }
                    val negativeCallback = {
                        showActions(
                            needsLocationPermission,
                            mustTurnOnLocationService,
                            BluetoothHelper.instance.needsBluetoothPermissions,
                            mustTurnOnBluetooth
                        )
                    }
                    showModalOptionAlert(
                        getString(R.string.user_action_required),
                        getString(R.string.manual_bluetooth),
                        getString(R.string.open_settings),
                        positiveCallback = positiveCallback,
                        negativeCallback = negativeCallback
                    )
                } else {
                    doRequestBluetoothPermissions()
                }
            }
        }
    }

    private fun checkAccountEmailVerification() {
        val intent = Intent(this, VerifyEmailActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun runChecks() {
        if (paused) {
            return
        }
        hideActions()
        if (needsLocationPermission) {
            askLocationPermission()
        } else if (mustTurnOnLocationService) {
            showActions(
                locationPermission = false,
                locationOff = true,
                BluetoothHelper.instance.needsBluetoothPermissions,
                mustTurnOnBluetooth
            )
        } else if (BluetoothHelper.instance.needsBluetoothPermissions) {
            askBluetoothPermissions()
        } else if (mustTurnOnBluetooth) {
            showActions(
                locationPermission = false,
                locationOff = false,
                bluetoothPermission = false,
                bluetoothOff = true
            )
        } else {
            checkAccountEmailVerification()
        }
    }
}
