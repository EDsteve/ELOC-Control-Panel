package de.eloc.eloc_control_panel.helpers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

public class DeviceInfo {
    public static final String DEFAULT_NAME = "<unknown device>";
    public static final String DEFAULT_ADDRESS = "00:00:00:00:00:00";

    public final String name;
    public final String address;

    public DeviceInfo(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public static DeviceInfo getDefault() {
        return new DeviceInfo(DEFAULT_NAME, DEFAULT_ADDRESS);
    }

    public static DeviceInfo fromDevice(BluetoothDevice device) {
        // it is safe to suppress permission,
        // because when this method is call, bluetooth permissions
        // will already be granted.
        if (device != null) {
            @SuppressLint("MissingPermission")
            String name = device.getName();
            if (name == null) {
                name = DeviceInfo.DEFAULT_NAME;
            }
            String address = device.getAddress();
            if (address == null) {
                address = DEFAULT_ADDRESS;
            }
            return new DeviceInfo(name, address);
        }
        return new DeviceInfo("<unknown device>", "<00:00:00:00:00:00>");

    }
}
