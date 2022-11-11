package de.eloc.eloc_control_panel.receivers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import de.eloc.eloc_control_panel.helpers.BluetoothHelper;
import de.eloc.eloc_control_panel.helpers.DeviceInfo;

public class BluetoothScanReceiver extends BroadcastReceiver {

    private final BluetoothHelper.ListUpdateCallback updateCallback;
    public BluetoothScanReceiver(BluetoothHelper.ListUpdateCallback callback) {
        this.updateCallback = callback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    BluetoothHelper.addDevice(device, updateCallback);
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    try {
                        String deviceName = DeviceInfo.fromDevice(device).name;
                        if (deviceName.toLowerCase().startsWith("eloc")) {
                            BluetoothHelper.addDevice(device, updateCallback);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                device.createBond();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("elocApp", "I got an error", e);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                BluetoothHelper.clearDevices();
                if (updateCallback != null) {
                    updateCallback.handler(true, false);
                }
                Log.d("TAG", "Started scanning... "  );
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothHelper.setReadyToScan(true);
                if (updateCallback != null) {
                    updateCallback.handler(BluetoothHelper.hasEmptyAdapter(), true);
                }
                Log.d("TAG", "Scanning completed "  );
            }
    }
}
