package de.eloc.eloc_control_panel.receivers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;

import de.eloc.eloc_control_panel.helpers.BluetoothHelper;

public class BluetoothScanReceiver extends BroadcastReceiver {

    private BluetoothHelper.ListUpdateCallback updateCallback;
    private final ArrayList<Integer> lock = new ArrayList<>();

    public BluetoothScanReceiver() {
        // Empty constructor required by manifest
    }

    public BluetoothScanReceiver(BluetoothHelper.ListUpdateCallback callback) {
        this.updateCallback = callback;
        lock.add(0);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {

        synchronized (lock) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    BluetoothHelper.addDevice(device, updateCallback);
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    try {
                        String deviceName = device.getName();
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

                lock.set(0, lock.get(0) + 1);
                Log.d("TAG", "Started scanning... " + lock.get(0));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothHelper.setReadyToScan(true);
                if (updateCallback != null) {
                    updateCallback.handler(BluetoothHelper.hasEmptyAdapter(), true);
                }
                lock.set(0, lock.get(0) + 1);
                Log.d("TAG", "Scanning completed " + lock.get(0));
            }
        }
    }
}
