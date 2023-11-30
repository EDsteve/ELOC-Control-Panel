package de.eloc.eloc_control_panel.ng3.data.helpers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity
import de.eloc.eloc_control_panel.ng3.activities.showModalAlert
import de.eloc.eloc_control_panel.ng3.interfaces.IntCallback
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val SCAN_DURATION = 30 // Seconds

object BluetoothHelper {
    val broadcastFilter: IntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    val enablingIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    private var scannerElapsed = 0
    private var executorHandle: ScheduledFuture<*>? = null
    private val bluetoothManager: BluetoothManager? =
        App.instance.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?

    val needsBluetoothPermissions: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                for (permission in bluetoothPermissions) {
                    val status = ContextCompat.checkSelfPermission(App.instance, permission)
                    if (status != PackageManager.PERMISSION_GRANTED) {
                        return true
                    }
                }
            }
            return false
        }

    val hasConnectPermission
        get(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val status = ActivityCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                return (status == PackageManager.PERMISSION_GRANTED)
            }
            return true
        }

    val bluetoothPermissions: Array<String>
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            else arrayOf()
        }

    val isAdapterOn
        get(): Boolean {
            return bluetoothManager?.adapter?.state == BluetoothAdapter.STATE_ON
        }

    val isBluetoothOn: Boolean
        get() {
            return bluetoothManager?.adapter?.isEnabled ?: false
        }

    private val hasAdapter: Boolean
        get() {
            return bluetoothManager?.adapter != null
        }

    val isScanning: Boolean
        get() {
            return isScanningBluetooth || (executorHandle != null)
        }

    private val isScanningBluetooth: Boolean
        get() {
            var scanning = false
            val adapter = bluetoothManager?.adapter
            if (adapter != null) {
                if (ContextCompat.checkSelfPermission(
                        App.instance,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    scanning = adapter.isDiscovering
                }
            }
            return scanning
        }

    fun openSettings(activity: ThemableActivity) {
        if (hasAdapter) {
            val intent = Intent()
            intent.action = Settings.ACTION_BLUETOOTH_SETTINGS
            activity.startActivity(intent)
        } else {
            activity.showModalAlert(
                activity.getString(R.string.bluetooth),
                activity.getString(R.string.no_bluetooth_adapter)
            )
        }
    }

    fun startScan(callback: IntCallback): String? {
        return if (isScanning)
            stopScan(callback)
        else
            startBluetoothScan(callback)
    }

    private fun startBluetoothScan(callback: IntCallback): String? {
        val adapter = bluetoothManager?.adapter
        if (adapter != null) {
            if (ContextCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return "Set bluetooth permissions in app settings!"
                }
            }
            scannerElapsed = 0
            executorHandle =
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    {
                        if (scannerElapsed >= SCAN_DURATION) {
                            stopScan(callback)
                        }
                        scannerElapsed++
                        callback.handler(SCAN_DURATION - scannerElapsed)
                    },
                    0,
                    1,
                    TimeUnit.SECONDS
                )
            adapter.cancelDiscovery()
            adapter.startDiscovery()
        }
        return null
    }

    fun getDevice(address: String): BluetoothDevice? =
        bluetoothManager?.adapter?.getRemoteDevice(address.uppercase(Locale.ENGLISH))

    fun stopScan(callback: IntCallback): String? {
        if (ContextCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return "Set bluetooth permissions in app settings!"
            }
        }
        bluetoothManager?.adapter?.cancelDiscovery()
        stopExecutor()
        callback.handler(-1)
        return null
    }

    private fun stopExecutor() {
        if (executorHandle != null) {
            executorHandle?.cancel(true)
        }
        executorHandle = null
    }
}