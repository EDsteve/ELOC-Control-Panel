package de.eloc.eloc_control_panel.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.interfaces.ConnectionStatusListener
import de.eloc.eloc_control_panel.interfaces.SocketListener
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

object DeviceDriver : Runnable {
    private var connecting = false
    private var disconnecting = false
    private const val NEWLINE = "\n"
    private const val EOT: Byte = 4
    private var bluetoothSocket: BluetoothSocket? = null
    private val INTENT_ACTION_DISCONNECT = App.applicationId + ".Disconnect"
    private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var device: BluetoothDevice? = null
    private var bytesCache = emptyList<Byte>()
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

    private fun notifyClients() {
        for (c in clients) {
            c.onStatusChanged(connectionStatus)
        }
    }

    fun disconnect() {
        disconnecting = true
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
        dataListenerList.clear()
        disconnecting = false
    }

    fun write(command: String) {
        try {
            if (bluetoothSocket?.isConnected == true) {
                val sanitizedCommand = command.trim() + NEWLINE
                val data = sanitizedCommand.encodeToByteArray()
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
        write("setRecordmode#mode=recordOff")
    }

    fun startRecording() {
        write("setRecordmode#mode=recordOn")
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


    fun registerConnectionChangedListener(callback: ConnectionStatusListener? = null) {
        if ((callback != null) && (!clients.contains(callback))) {
            clients.add(callback)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(
        deviceAddress: String,
        dataListener: SocketListener? = null
    ) {
        connecting = true
        var hadError = false
        try {
            disconnect()
            connectionStatus = ConnectionStatus.Pending
            device = BluetoothHelper.getDevice(deviceAddress)

            // Connection success and most connection errors are returned asynchronously to listener
            try {
                App.instance.registerReceiver(
                    disconnectBroadcastReceiver, IntentFilter(INTENT_ACTION_DISCONNECT)
                )
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(BLUETOOTH_SPP)
                BluetoothHelper.stopScan {
                    doConnect()
                }
            } catch (e: Exception) {
                // todo : remove
                println(e.localizedMessage)
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
            connecting = false
        }
    }

    private fun doConnect() {
        try {
            bluetoothSocket?.connect()
        } catch (e: SecurityException) {
            onConnectionError(e)
            disconnect()
            return
        }

        connectionStatus =
            if (bluetoothSocket?.isConnected == true) {
                ConnectionStatus.Active
            } else {
                ConnectionStatus.Inactive
            }
        if (connectionStatus == ConnectionStatus.Active) {
            onConnect()
            Executors.newSingleThreadExecutor().submit(this)
        } else {
            return
        }
    }

    override fun run() {
        try {
            val buffer = ByteArray(1024)
            while (bluetoothSocket?.isConnected == true) {
                try {
                    val byteCount = bluetoothSocket?.inputStream?.read(buffer) ?: 0
                    val data = buffer.copyOf(byteCount)
                    bytesCache += data.toList()
                    while (true) {
                        val offset = bytesCache.indexOf(EOT)
                        if (offset < 0) {
                            break
                        }
                        val jsonBytes = bytesCache.slice(0 until offset)
                        val startIndex = offset + 1
                        val endIndex = bytesCache.size - 1
                        bytesCache = bytesCache.slice(startIndex..endIndex)
                        onRead(jsonBytes.toByteArray())
                    }
                } catch (e: Exception) {
                    if (connecting || disconnecting) {
                        // Ignore and don't report the crash that happens while connecting
                        return
                    }

                    onIOError(e)
                    disconnect()
                }
                try {
                    Thread.sleep(500)
                } catch (_: Exception) {
                }
            }

        } catch (e: SecurityException) {
            onConnectionError(e)
            disconnect()
            return
        }
    }

    fun getStatus() {
        write("getStatus")
    }

    fun getConfig() {
        write("getConfig")
    }
}