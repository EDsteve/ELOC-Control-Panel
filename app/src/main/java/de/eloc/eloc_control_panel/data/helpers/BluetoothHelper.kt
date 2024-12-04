package de.eloc.eloc_control_panel.data.helpers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.themable.ThemableActivity
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.interfaces.IntCallback
import de.eloc.eloc_control_panel.interfaces.VoidCallback
import de.eloc.eloc_control_panel.data.AssociatedDeviceInfo
import de.eloc.eloc_control_panel.data.BtDevice
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
                    Manifest.permission.BLUETOOTH,
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
                Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
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

    private fun isDeviceAssociated(context: Context, devAddress: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingAssociations = associatedDevices(context)
            for (info in existingAssociations) {
                if (info.mac.uppercase() == devAddress.uppercase()) {
                    return true
                }
            }
            return false
        } else {
            return true
        }
    }

    fun changeName(device: BluetoothDevice?, name: String) {
        try {
            if (device == null) {
                Logger.d("BluetoothDevice is null.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        App.instance,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Logger.d("BLUETOOTH_CONNECT permission not granted.")
                    return
                }

                Logger.d("Attempting to set alias to $name for device ${device.name}")
                device.setAlias(name)  // Critical line that could crash
                Logger.d("Alias successfully set to $name for device ${device.name}")
            } else {
                Logger.d("setAlias is not supported on devices below Android 13.")
            }
        } catch (securityException: SecurityException) {
            Logger.d("Permission issue: ${securityException.localizedMessage}")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Logger.d("Invalid argument: ${illegalArgumentException.localizedMessage}")
        } catch (unsupportedOperationException: UnsupportedOperationException) {
            Logger.d("Operation not supported: ${unsupportedOperationException.localizedMessage}")
        } catch (e: Exception) {
            Logger.d("Unexpected error: ${e.localizedMessage}")
        }
    }

    fun associatedDevices(context: Context): List<AssociatedDeviceInfo> {
        val existingAssociations = mutableListOf<AssociatedDeviceInfo>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val deviceManager =
                context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager?
            if (deviceManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    for (info in deviceManager.myAssociations) {
                        val assocInfo = AssociatedDeviceInfo(
                            associationId = info.id,
                            name = info.displayName.toString(),
                            mac = info.deviceMacAddress.toString().uppercase()
                        )
                        existingAssociations.add(assocInfo)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    for (assoc in deviceManager.associations) {
                        val assocInfo = AssociatedDeviceInfo(
                            mac = assoc.uppercase(),
                            name = "<unknown device>",
                            associationId = null
                        )
                        existingAssociations.add(assocInfo)
                    }
                }
            }
        }
        return existingAssociations
    }

    fun disassociateDevice(context: Context, info: AssociatedDeviceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val deviceManager =
                context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager?
            if (deviceManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (info.associationId != null) {
                        deviceManager.disassociate(info.associationId)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    deviceManager.disassociate(info.mac)
                    DataManager.removeAssociation(info.mac)
                }
            }
        }
    }

    fun associateDevice(
        context: Context, device: BtDevice,
        associationLauncher: ActivityResultLauncher<IntentSenderRequest>,
        associationCompletedCallback: VoidCallback,
    ) {
        if (isDeviceAssociated(context, device.address)) {
            associationCompletedCallback.handler()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val deviceManager =
                context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager?
            if (deviceManager != null) {
                val filter = BluetoothDeviceFilter.Builder()
                    .setAddress(device.address)
                    .build()

                val associationRequest = AssociationRequest.Builder()
                    .addDeviceFilter(filter)
                    .setSingleDevice(true)
                    .build()

                val associationCallback = object : CompanionDeviceManager.Callback() {
                    override fun onAssociationCreated(associationInfo: AssociationInfo) {
                        super.onAssociationCreated(associationInfo)
                        Logger.d("Association created")
                        DataManager.addAssociation(device.name, device.address)
                        associationCompletedCallback.handler()
                    }

                    override fun onAssociationPending(intentSender: IntentSender) {
                        super.onAssociationPending(intentSender)
                        Logger.d("Association pending")
                        val senderRequest = IntentSenderRequest.Builder(intentSender).build()
                        associationLauncher.launch(senderRequest)
                    }

                    override fun onFailure(p0: CharSequence?) {
                        Logger.d("Association failed")
                    }
                }

                deviceManager.associate(
                    associationRequest,
                    associationCallback,
                    Handler(Looper.getMainLooper())
                )
            }
        } else {
            associationCompletedCallback.handler()
        }
    }
}