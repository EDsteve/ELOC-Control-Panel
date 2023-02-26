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
import de.eloc.eloc_control_panel.ng2.interfaces.ListUpdateCallback;
import de.eloc.eloc_control_panel.ng.models.AppBluetoothManager;

public class BluetoothScanReceiver extends BroadcastReceiver {

    private final ListUpdateCallback updateCallback;
    public BluetoothScanReceiver(ListUpdateCallback callback) {
        this.updateCallback = callback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    AppBluetoothManager.INSTANCE.addDevice(device, updateCallback);
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    try {
                        if (AppBluetoothManager.INSTANCE.isElocDevice(device)) {
                            AppBluetoothManager.INSTANCE.addDevice(device, updateCallback);
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
                AppBluetoothManager.INSTANCE.clearDevices();
                if (updateCallback != null) {
                    updateCallback.handler(true, false);
                }
                Log.d("TAG", "Started scanning... "  );
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                AppBluetoothManager.INSTANCE.setReadyToScan(true);
                if (updateCallback != null) {
                  updateCallback.handler(AppBluetoothManager.INSTANCE.hasEmptyAdapter(), true);
                }
                Log.d("TAG", "Scanning completed "  );
            }
    }
}
