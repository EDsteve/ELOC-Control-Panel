package de.eloc.eloc_control_panel.ng3.interfaces

interface SocketListener {
    fun onConnect()
    fun onConnectionError(e: Exception)
    fun onRead(data: ByteArray)
    fun onIOError(e: Exception)
}

abstract class BluetoothDeviceListener : SocketListener {
    override fun onConnect() {}
    override fun onConnectionError(e: Exception) {}
    override fun onIOError(e: Exception) {}
    override fun onRead(data: ByteArray) {}
}

