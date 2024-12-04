package de.eloc.eloc_control_panel.activities.themable

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.openLocationSettings
import de.eloc.eloc_control_panel.activities.openSystemAppSettings
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityPermissionsSetupBinding
import de.eloc.eloc_control_panel.receivers.ElocReceiver
import java.lang.Thread.sleep

// TODO: wait for internet status to be non-null

class PermissionsSetupActivity : ThemableActivity() {
    private var paused = false
        set(value) {
            field = value
            if (field) {
                hideActions()
            } else {
                showActions()
            }
        }
    private lateinit var binding: ActivityPermissionsSetupBinding
    private val elocReceiver = ElocReceiver(this::runChecks, null)
    private var stopChecks = false
    private var checksThread: Thread? = null

    private val needsLocationPermission1: Boolean
        get() {
            val fineLocationGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            return (fineLocationGranted != PackageManager.PERMISSION_GRANTED)
        }

    private val needsNotificationsPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status =
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            (status != PackageManager.PERMISSION_GRANTED)
        } else {
            false
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
        get() = !BluetoothHelper.isBluetoothOn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        elocReceiver.register(this)
    }

    override fun onPause() {
        super.onPause()
        paused = true
        stopChecks = true
    }

    override fun onResume() {
        super.onResume()
        paused = false
        checksThread = Thread {
            while (true) {
                runChecks()
                sleep(500)
                if (stopChecks) {
                    checksThread = null
                    break
                }
            }
        }
        checksThread?.start()
    }

    override fun onDestroy() {
        elocReceiver.unregister(this)
        super.onDestroy()
    }

    private fun setListeners() {
        binding.bluetoothButton.setOnClickListener { openSystemAppSettings() }
        binding.locationButton.setOnClickListener { openSystemAppSettings() }
        binding.locationStateButton.setOnClickListener { openLocationSettings() }
        binding.notificationsButton.setOnClickListener { openSystemAppSettings() }
        binding.bluetoothStateButton.setOnClickListener { startActivity(BluetoothHelper.enablingIntent) }
    }

    private fun hideActions() {
        binding.title.visibility = View.INVISIBLE
        binding.locationButton.visibility = View.INVISIBLE
        binding.bluetoothButton.visibility = View.INVISIBLE
        binding.locationStateButton.visibility = View.INVISIBLE
        binding.bluetoothStateButton.visibility = View.INVISIBLE
        binding.notificationsButton.visibility = View.INVISIBLE
    }

    private fun showActions() {
        val locationPermission = needsLocationPermission1
        val locationOff = mustTurnOnLocationService
        val bluetoothPermission = BluetoothHelper.needsBluetoothPermissions
        val bluetoothOff = mustTurnOnBluetooth
        val notificationsPermission = needsNotificationsPermission

        if (!locationPermission && !locationOff && !bluetoothPermission && !bluetoothOff && !notificationsPermission) {
            return
        }
        binding.title.visibility = View.VISIBLE
        binding.locationButton.visibility = if (locationPermission) View.VISIBLE else View.GONE
        binding.bluetoothButton.visibility = if (bluetoothPermission) View.VISIBLE else View.GONE
        binding.locationStateButton.visibility = if (locationOff) View.VISIBLE else View.GONE
        binding.bluetoothStateButton.visibility = if (bluetoothOff) View.VISIBLE else View.GONE
        binding.notificationsButton.visibility =
            if (notificationsPermission) View.VISIBLE else View.GONE
    }

    private fun doRequestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Preferences.setLocationRequested()
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
    }

    private fun doRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Preferences.setBluetoothRequested()
            requestPermissions(BluetoothHelper.bluetoothPermissions, 0)
        }
    }

    private fun doRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Preferences.setNotificationsRequested()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    private fun askLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val negativeCallback = { showActions() }
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                val appName = getString(R.string.app_name)
                val rationale = getString(R.string.location_service_rationale, appName)
                paused = true
                val positiveCallback = {
                    doRequestLocationPermission()
                }
                showModalOptionAlert(
                    title = getString(R.string.permission_required),
                    message = rationale,
                    positiveButtonLabel = getString(R.string.grant_permission),
                    positiveCallback = positiveCallback,
                    negativeCallback = negativeCallback
                )
            } else {
                if (Preferences.locationRequested) {
                    paused = true
                    val positiveCallback = {
                        openSystemAppSettings()
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
            for (permission in BluetoothHelper.bluetoothPermissions) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    showRationale = true
                    break
                }
            }
            if (showRationale) {
                val appName = getString(R.string.app_name)
                val message = getString(R.string.bluetooth_rationale, appName)
                paused = true
                val positiveCallback = { doRequestBluetoothPermissions() }
                val negativeCallback = { showActions() }
                showModalOptionAlert(
                    getString(R.string.permission_required),
                    message,
                    getString(R.string.grant_permission),
                    positiveCallback = positiveCallback,
                    negativeCallback = negativeCallback
                )
            } else {
                if (Preferences.bluetoothRequested) {
                    paused = true
                    val positiveCallback = { openSystemAppSettings() }
                    val negativeCallback = { showActions() }
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

    private fun askNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val showRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            if (showRationale) {
                val appName = getString(R.string.app_name)
                val message = getString(R.string.notifications_rationale, appName)
                paused = true
                val positiveCallback = { doRequestNotificationsPermission() }
                val negativeCallback = { showActions() }
                showModalOptionAlert(
                    getString(R.string.permission_required),
                    message,
                    getString(R.string.grant_permission),
                    positiveCallback = positiveCallback,
                    negativeCallback = negativeCallback
                )
            } else {
                if (Preferences.notificationsRequested) {
                    paused = true
                    val positiveCallback = {
                        openSystemAppSettings()
                    }
                    val negativeCallback = { showActions() }
                    showModalOptionAlert(
                        getString(R.string.user_action_required),
                        getString(R.string.manual_notifications),
                        getString(R.string.open_settings),
                        positiveCallback = positiveCallback,
                        negativeCallback = negativeCallback
                    )
                } else {
                    doRequestNotificationsPermission()
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
        runOnUiThread {
            if (!paused) {
                hideActions()
                if (needsLocationPermission1) {
                    askLocationPermission()
                } else if (mustTurnOnLocationService) {
                    showActions()
                } else if (BluetoothHelper.needsBluetoothPermissions) {
                    askBluetoothPermissions()
                } else if (mustTurnOnBluetooth) {
                    showActions()
                } else if (needsNotificationsPermission) {
                    askNotificationsPermission()
                } else {
                    checkAccountEmailVerification()
                }
            }
        }
    }
}
