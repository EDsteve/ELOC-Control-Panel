package de.eloc.eloc_control_panel.ng.models

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import de.eloc.eloc_control_panel.ng2.App
import de.eloc.eloc_control_panel.ng.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.ng2.interfaces.ListUpdateCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

object AppBluetoothManager {
    private var readyToScan = false
    private var devices = ArrayList<BluetoothDevice>()
    private var adapter =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val bluetoothManager =
                App.instance.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            bluetoothManager?.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }

    fun getDeviceCount(): Int = devices.size

    fun getDevice(index: Int): BluetoothDevice = devices[index]

    fun getDevice(address: String): BluetoothDevice? {
        return adapter?.getRemoteDevice(address.uppercase(Locale.ENGLISH))
    }

    fun hasEmptyAdapter(): Boolean {
        return devices.isEmpty()
    }

    fun isAdapterOn(): Boolean {
        return adapter?.state == BluetoothAdapter.STATE_ON
    }

    private fun isReadyToScan(): Boolean {
        return readyToScan
    }

    fun stopScan(): Boolean {
        val app = App.instance
        if (ActivityCompat.checkSelfPermission(
                app,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are checked when app is launched (by SetupActivity)
            return false
        }
        setReadyToScan(false)
        var stopped = true
        if (adapter?.isDiscovering == true) {
            stopped = adapter?.cancelDiscovery() ?: false
        } else {
            setReadyToScan(true)
        }
        return stopped
    }

    fun setReadyToScan(ready: Boolean) {
        readyToScan = ready
    }


    @SuppressLint("MissingPermission")
    fun isElocDevice(device: BluetoothDevice): Boolean {
        var deviceName = device.name
        if (deviceName == null) {
            deviceName = ""
        }
        return deviceName.trim().lowercase().startsWith("eloc")
    }

    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice, callback: ListUpdateCallback) {
        if (!isElocDevice(device)) {
            return
        }
        if (!devices.contains(device)) {
            devices.add(device)
            devices.sortWith(compareByDescending { it.name })

            System.out.println("traceeeeeeeeADDCALLED" + devices.get(0).getName());

            // todo            listAdapter.notifyDataSetChanged();
            callback.handler(devices.isEmpty(), false)
        }
    }

    fun scanAsync(callback: BooleanCallback) {
        // Don't start scanning until 'readyToScan' is true
        // 'readyToScan' must be set by the receiver
        // (after an event for scan finished hasbeen broadcast) or by stopScan()
        // This is important to maintain proper UI state.
        val stopped = stopScan()
        GlobalScope.launch {
            while (true) {
                if (isReadyToScan()) {
                    var started = false
                    if (stopped) {
                        // startDiscovery() will scan for 120 seconds max.
                        if (ActivityCompat.checkSelfPermission(
                                App.instance,
                                Manifest.permission.BLUETOOTH_SCAN
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            started = adapter?.startDiscovery() ?: false
                        }
                    }

                    callback.handler(started)
                    break
                } else {
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                    }
                }
            }
        }
    }

    fun clearDevices() = devices.clear()

}
