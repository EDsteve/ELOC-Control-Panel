package de.eloc.eloc_control_panel.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.eloc.eloc_control_panel.helpers.BluetoothHelper;
import de.eloc.eloc_control_panel.ng.models.AppBluetoothManager;

public class BluetoothStateReceiver extends BroadcastReceiver {
    interface StateChangedCallback {
        void handler(boolean isOn);
    }

    private final StateChangedCallback callback;

    public BluetoothStateReceiver(StateChangedCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean stateChanged = BluetoothAdapter.ACTION_STATE_CHANGED.equals(action);
        if (stateChanged) {
            boolean isOn = AppBluetoothManager.INSTANCE.isAdapterOn();
            if (callback != null) {
                callback.handler(isOn);
            }
        }
    }
}
