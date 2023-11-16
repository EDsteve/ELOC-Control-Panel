package de.eloc.eloc_control_panel.ng3

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.eloc.eloc_control_panel.ng.models.BluetoothHelperOld
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper
import de.eloc.eloc_control_panel.ng3.data.ConnectionStatus
import de.eloc.eloc_control_panel.ng3.interfaces.ConnectionStatusListener
import de.eloc.eloc_control_panel.ng3.interfaces.SocketListener
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

object DeviceDriver : Runnable {
    private var bluetoothSocket: BluetoothSocket? = null
    private val INTENT_ACTION_DISCONNECT = App.applicationId + ".Disconnect"
    private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var device: BluetoothDevice? = null
    private val clients = mutableListOf<ConnectionStatusListener>()
    private val dataListenerList = mutableListOf<SocketListener>()
    private val disconnectBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onIOError(IOException("ELOC bluetooth disconnected in background!"))

            // disconnect now, else would be queued until UI re-attached
            disconnect()
        }
    }

    val name
        get() : String {
            var buffer = try {
                device?.name?.trim() ?: ""
            } catch (_: SecurityException) {
                ""
            }
            if (buffer.isEmpty()) {
                buffer = device?.address?.trim() ?: ""
            }
            if (buffer.isEmpty()) {
                buffer = "<invalid device>"
            }
            return buffer
        }

    private var connectionStatus = ConnectionStatus.Inactive
        set(value) {
            field = value
            notifyClients()
        }

    fun connect(
        deviceAddress: String,
        callback: ConnectionStatusListener? = null,
        dataListener: SocketListener? = null, test: Boolean
    ) {
        if (test) {
            connectionStatus = ConnectionStatus.Active
            if ((callback != null) && (!clients.contains(callback))) {
                clients.add(callback)
            }
            if ((dataListener != null) && (!dataListenerList.contains(dataListener))) {
                dataListenerList.add(dataListener)
            }
            return
        }
        var hadError = false
        try {
            disconnect()
            connectionStatus = ConnectionStatus.Pending
            device = BluetoothHelperOld.getDevice(deviceAddress)

            // Connection success and most connection errors are returned asynchronously to listener
            try {
                App.instance.registerReceiver(
                    disconnectBroadcastReceiver, IntentFilter(INTENT_ACTION_DISCONNECT)
                )
                Executors.newSingleThreadExecutor().submit(this)
            } catch (_: Exception) {

            }

            if ((callback != null) && (!clients.contains(callback))) {
                clients.add(callback)
            }
            if ((dataListener != null) && (!dataListenerList.contains(dataListener))) {
                dataListenerList.add(dataListener)
            }
        } catch (e: Exception) {
            hadError = true
        } finally {
            connectionStatus =
                if (bluetoothSocket?.isConnected == true) {
                    ConnectionStatus.Active
                } else if (hadError) {
                    ConnectionStatus.Inactive
                } else {
                    ConnectionStatus.Pending
                }
        }
    }

    private fun notifyClients() {
        for (c in clients) {
            c.onStatusChanged(connectionStatus)
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {
        }
        bluetoothSocket = null
        try {
            App.instance.unregisterReceiver(disconnectBroadcastReceiver)
        } catch (_: Exception) {
        }
        connectionStatus = ConnectionStatus.Inactive
        clients.clear()
        dataListenerList.clear()
    }

    fun write(command: String) {
        try {
            if (bluetoothSocket?.isConnected == true) {
                val data = command.encodeToByteArray()
                bluetoothSocket?.outputStream?.write(data)
            } else {
                throw IOException("ELOC device not connected!")
            }
        } catch (e: IOException) {
            disconnect()
            onIOError(e)
        }
    }

    fun stopRecording() {
        write("setRecordMode#mode=stop")
    }

    private fun onConnect() {
        for (l in dataListenerList) {
            l.onConnect()
        }
    }

    private fun onConnectionError(e: Exception) {
        for (l in dataListenerList) {
            l.onConnectionError(e)
        }
    }

    private fun onRead(data: ByteArray) {
        for (l in dataListenerList) {
            l.onRead(data)
        }
    }

    private fun onIOError(e: Exception) {
        for (l in dataListenerList) {
            l.onIOError(e)
        }
    }

    override fun run() {
        try {
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP)
            BluetoothHelper.instance.stopScan {
                try {
                    bluetoothSocket?.connect()
                } catch (e: SecurityException) {
                    onConnectionError(e)
                    disconnect()
                    return@stopScan
                }

                connectionStatus =
                    if (bluetoothSocket?.isConnected == true) {
                        ConnectionStatus.Active
                    } else {
                        ConnectionStatus.Inactive
                    }
                if (connectionStatus == ConnectionStatus.Active) {
                    onConnect()
                } else {
                    return@stopScan
                }

                val buffer = ByteArray(1024)
                while (bluetoothSocket?.isConnected == true) {
                    try {
                        val byteCount = bluetoothSocket?.inputStream?.read(buffer) ?: 0
                        val data = buffer.copyOf(byteCount)
                        onRead(data)
                    } catch (e: Exception) {
                        onIOError(e)
                        disconnect()
                    }
                    try {
                        Thread.sleep(500)
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: SecurityException) {
            onConnectionError(e)
            disconnect()
            return
        }
    }

    // todo: delete
    fun dummyGetConfig() {
        val config = "{\n" +
                "  \"ecode\": 0,\n" +
                "  \"cmd\": \"getConfig\",\n" +
                "  \"payload\": {\n" +
                "    \"device\": {\n" +
                "      \"location\": \"London\",\n" +
                "      \"locationCode\": \"4534985893+QR\",\n" +
                "      \"locationAccuracy\": \"93\",\n" +
                "      \"nodeName\": \"ELOC_NONAME\"\n" +
                "    },\n" +
                "    \"config\": {\n" +
                "      \"secondsPerFile\": 60,\n" +
                "      \"listenOnly\": false,\n" +
                "      \"cpuMaxFrequencyMHZ\": 80,\n" +
                "      \"cpuMinFrequencyMHZ\": 10,\n" +
                "      \"cpuEnableLightSleep\": true,\n" +
                "      \"bluetoothEnableAtStart\": false,\n" +
                "      \"bluetoothEnableOnTapping\": true,\n" +
                "      \"bluetoothEnableDuringRecord\": true,\n" +
                "      \"bluetoothOffTimeoutSeconds\": 60\n" +
                "    },\n" +
                "    \"mic\": {\n" +
                "      \"MicType\": \"ICS-434349\",\n" +
                "      \"MicBitShift\": 11,\n" +
                "      \"MicSampleRate\": 26000,\n" +
                "      \"MicUseAPLL\": true,\n" +
                "      \"MicUseTimingFix\": true,\n" +
                "      \"MicGPSCoords\": \"ns\",\n" +
                "      \"MicPointingDirectionDegrees\": \"ns\",\n" +
                "      \"MicHeight\": \"ns\",\n" +
                "      \"MicMountType\": \"ns\"\n" +
                "    }\n" +
                "  }\n" +
                "}"
        onRead(config.encodeToByteArray())
    }

    // todo: delete
    fun dummyGetStatus() {
        val status = "{\n" +
                "  \"ecode\": 0,\n" +
                "  \"cmd\": \"getStatus\",\n" +
                "  \"payload\": {\n" +
                "    \"battery\": {\n" +
                "      \"type\": \"LiPo Battery\",\n" +
                "      \"state\": \"Full\",\n" +
                "      \"SoC[%]\": 4.309902668,\n" +
                "      \"voltage[V]\": 3.563073397\n" +
                "    },\n" +
                "    \"session\": {\n" +
                "      \"identifier\": \"ELOC2312647821\",\n" +
                "      \"recordingState\": \"0\",\n" +
                "      \"recordingTime[h]\": 1.23\n" +
                "    },\n" +
                "    \"device\": {\n" +
                "      \"firmware\": \"ELOC_V0.1\",\n" +
                "      \"timeStamp\": \"0.54\",\n" +
                "      \"totalRecordingTime[h]\": \"34.56\",\n" +
                "      \"SdCardFreeSpace[GB]\": \"115.58\"\n" +
                "    }\n" +
                "  }\n" +
                "}"
        onRead(status.encodeToByteArray())
    }
}