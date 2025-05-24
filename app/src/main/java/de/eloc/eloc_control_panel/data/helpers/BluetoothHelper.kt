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
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.themable.ThemableActivity
import de.eloc.eloc_control_panel.data.AssociatedDeviceInfo
import de.eloc.eloc_control_panel.data.BtDevice
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

private const val DEFAULT_SCAN_DURATION = 15 // Seconds

object BluetoothHelper {
    val enablingIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    var scanningSpecificEloc = false
    private var scannerElapsed = 0
    private var scannerHandle: Any? = null
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
            return bluetoothManager?.adapter?.isEnabled == true
        }

    private val hasAdapter: Boolean
        get() {
            return bluetoothManager?.adapter != null
        }

    val isScanning: Boolean
        get() {
            return isScanningBluetooth || (scannerHandle != null)
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

    fun startScan(
        updateCallback: ((Int) -> Unit)?,
        completedCallback: (String?) -> Unit
    ) {
        return if (isScanning)
            stopScan(updateCallback, completedCallback)
        else
            startBluetoothScan(updateCallback, completedCallback)
    }

    private fun startBluetoothScan(
        updateCallback: ((Int) -> Unit)?,
        completedCallback: (String?) -> Unit
    ) {
        // This method blocks UI. Run on executor/thread
        Executors.newSingleThreadExecutor().execute {
            val adapter = bluetoothManager?.adapter
            if (adapter != null) {
                if (ContextCompat.checkSelfPermission(
                        App.instance,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        completedCallback("Set bluetooth permissions in app settings!")
                        return@execute
                    }
                }
                scannerElapsed = 0
                Thread {
                    scannerHandle = Object()
                    while (scannerHandle != null) {
                        if (scannerElapsed >= DEFAULT_SCAN_DURATION) {
                            stopScan(updateCallback, completedCallback)
                            scannerHandle = null
                        } else {
                            scannerElapsed++
                            updateCallback?.invoke(DEFAULT_SCAN_DURATION - scannerElapsed)
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (_: Exception) {
                        }
                    }
                }.start()
                adapter.cancelDiscovery()
                adapter.startDiscovery()
            }
            completedCallback(null)
            return@execute
        }
    }

    fun getDevice(address: String): BluetoothDevice? =
        bluetoothManager?.adapter?.getRemoteDevice(address.uppercase(Locale.ENGLISH))

    fun stopScan(updateCallback: ((Int) -> Unit)?, completedCallback: ((String?) -> Unit)? = null) {
        if (scanningSpecificEloc) {
            completedCallback?.invoke(null)
            return
        }
        if (ContextCompat.checkSelfPermission(
                App.instance,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                completedCallback?.invoke("Set bluetooth permissions in app settings!")
                return
            }
        }
        bluetoothManager?.adapter?.cancelDiscovery()
        stopExecutor()
        updateCallback?.invoke(-1)
        completedCallback?.invoke(null)
        return
    }

    private fun stopExecutor() {
        if (scannerHandle != null) {
            scannerHandle = null
        }
    }

    private fun isDeviceAssociated(context: Context, devAddress: String): Boolean {
        // NOTE: Even though associations may exist for API Level lower than S,
        // we will only check for S and up. See comments in associateDevice() for details.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        App.instance,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                device.setAlias(name)  // Critical line that could crash
            }
        } catch (_: SecurityException) {

        } catch (_: IllegalArgumentException) {

        } catch (_: UnsupportedOperationException) {

        } catch (_: Exception) {

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
        associationCompletedCallback: () -> Unit,
    ) {
        // Association requirements are only required on Android 12+
        // (API level greater than R)
        // Implementation of the CompanionDeviceManager seems to be unpredictable
        // among the various OEM/vendors. So to lessen issues that can lead to
        // AbstractMethodError crashes (which cannot be caught), only use CDM for
        // Android 12+ (S and up)
        if (isDeviceAssociated(context, device.address)) {
            associationCompletedCallback()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                        DataManager.addAssociation(device.name, device.address)
                        associationCompletedCallback()
                    }

                    override fun onAssociationPending(intentSender: IntentSender) {
                        super.onAssociationPending(intentSender)
                        val senderRequest = IntentSenderRequest.Builder(intentSender).build()
                        associationLauncher.launch(senderRequest)
                    }

                    override fun onFailure(p0: CharSequence?) {
                    }
                }

                deviceManager.associate(
                    associationRequest,
                    associationCallback,
                    Handler(Looper.getMainLooper())
                )
            }
        } else {
            associationCompletedCallback()
        }
    }
}