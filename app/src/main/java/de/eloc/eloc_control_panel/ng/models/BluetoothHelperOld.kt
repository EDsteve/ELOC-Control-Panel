package de.eloc.eloc_control_panel.ng.models
/*
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import de.eloc.eloc_control_panel.databinding.DeviceListItemBinding
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng3.interfaces.AdapterItemCallback
import de.eloc.eloc_control_panel.ng3.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper
import java.util.*
import java.util.concurrent.Executors


object BluetoothHelperOld {

    private var devices = mutableListOf<BluetoothDevice>()
    private var listAdapter: ArrayAdapter<BluetoothDevice>? = null

    val scanFilter: IntentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        .apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        }

    fun initializeListAdapter(
        context: Context,
        callback: AdapterItemCallback?
    ): ArrayAdapter<BluetoothDevice> {
        listAdapter = listAdapter ?: ArrayAdapter(context, 0, devices)
            .apply {
                fun getView(position: Int, view: View, parent: ViewGroup): View {
                    val inflater = LayoutInflater.from(parent.context)
                    val binding = DeviceListItemBinding.bind(view)
                    val info = getDeviceInfo(position)
                    binding.text1.text = info.name
                    binding.text2.text = "fix this : " + info.address
                    binding.root.setOnClickListener {
                        callback?.handler(info.name, info.address)
                    }
                    return binding.root
                }
            }
        return listAdapter!!
    }

    private fun getDeviceInfo(index: Int): DeviceInfo {
        if ((index >= 0) && (index < devices.size)) {
            val device = devices[index]
            return DeviceInfo.fromDevice(device)
        }
        return DeviceInfo.default
    }

    var readyToScan = false


    fun getDeviceCount(): Int = devices.size

    fun getDevice(index: Int): BluetoothDevice = devices[index]



    val isAdapterInitialized: Boolean
        get() = (adapter != null)

    fun stopScan(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are checked when app is launched (by SetupActivity)
            return false
        }
        readyToScan = false
        var stopped = true
        if (adapter?.isDiscovering == true) {
            stopped = adapter?.cancelDiscovery() ?: false
        } else {
            readyToScan = true
        }
        return stopped
    }

    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice, callback: BooleanCallback) {
        if (!BluetoothHelper.instance.isElocDevice(device)) {
            return
        }
        if (!devices.contains(device)) {
            devices.add(device)
            devices.sortWith(compareByDescending { it.name.lowercase() })

            println("traceeeeeeeeADDCALLED" + devices[0].name)

            listAdapter?.notifyDataSetChanged()
            callback.handler(false)
        }
    }

    fun hasEmptyAdapter(): Boolean = listAdapter?.isEmpty ?: true

    fun scanAsync(context: Context, callback: BooleanCallback) {
        // Don't start scanning until 'readyToScan' is true
        // 'readyToScan' must be set by the receiver
        // (after an event for scan finished hasbeen broadcast) or by stopScan()
        // This is important to maintain proper UI state.
        val stopped = stopScan(context)
        Executors.newSingleThreadExecutor().execute {
            while (true) {
                if (readyToScan) {
                    var started = false
                    if (stopped) {
                        // startDiscovery() will scan for 120 seconds max.
                        if (ActivityCompat.checkSelfPermission(
                                context,
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
                        Log.d("TAG", "scanAsync: waiting for cancel scan event...");
                    } catch (_: InterruptedException) {
                    }
                }
            }
        }
    }

    fun clearDevices() = devices.clear()
    }
 */
