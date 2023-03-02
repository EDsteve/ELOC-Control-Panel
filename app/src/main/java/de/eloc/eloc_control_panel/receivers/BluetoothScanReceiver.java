package de.eloc.eloc_control_panel.receivers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.eloc.eloc_control_panel.ng.models.BluetoothHelperOld;
import de.eloc.eloc_control_panel.ng2.interfaces.BooleanCallback;
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper;

public class BluetoothScanReceiver extends BroadcastReceiver {

    private final BooleanCallback updateCallback;

    public BluetoothScanReceiver(BooleanCallback callback) {
        this.updateCallback = callback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                BluetoothHelperOld.INSTANCE.addDevice(device, updateCallback);
            } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                try {
                    if (BluetoothHelper.Companion.getInstance().isElocDevice(device)) {
                        BluetoothHelperOld.INSTANCE.addDevice(device, updateCallback);
                        device.createBond();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("elocApp", "I got an error", e);
                }
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            BluetoothHelperOld.INSTANCE.clearDevices();
            if (updateCallback != null) {
                updateCallback.handler(false);
            }
            Log.d("TAG", "Started scanning... ");
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            BluetoothHelperOld.INSTANCE.setReadyToScan(true);
            if (updateCallback != null) {
                updateCallback.handler(true);
            }
            Log.d("TAG", "Scanning completed ");
        }
    }
}
