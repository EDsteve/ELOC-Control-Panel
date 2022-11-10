package de.eloc.eloc_control_panel.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import de.eloc.eloc_control_panel.databinding.DeviceListItemBinding;

public class BluetoothHelper {
    private static BluetoothAdapter adapter;
    private static BluetoothHelper instance;
    private static ArrayAdapter<BluetoothDevice> listAdapter;
    private static boolean readyToScan = false;

    public static final ArrayList<BluetoothDevice> devices = new ArrayList<>();

    public interface ListAdapterCallback {
        void handler(String s);
    }

    public interface ListUpdateCallback {
        void handler(boolean isEmpty, boolean scanFinished);
    }

    public interface BooleanCallback {
        void handler(boolean b);
    }

    static {
        initialize();
    }

    public static void setReadyToScan(boolean ready) {
        readyToScan = ready;
    }

    public static boolean isReadyToScan() {
        return readyToScan;
    }

    public static boolean hasEmptyAdapter() {
        if (listAdapter != null) {
            return listAdapter.isEmpty();
        }
        return true; // Use empty as default state so app shows scan mode
    }

    public static void initialize() {
        if (adapter == null) {
            adapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (instance == null) {
            instance = new BluetoothHelper();
        }
    }

    @SuppressLint("MissingPermission")
    public static void addDevice(BluetoothDevice device, ListUpdateCallback callback) {
        if (!devices.contains(device)) {
            devices.add(device);
            System.out.println("traceeeeeeeeADDCALLED" + devices.get(0).getName().toString());
            listAdapter.notifyDataSetChanged();
            if (callback != null) {
                callback.handler(devices.isEmpty(), false);
            }
        }
    }

    public static ArrayAdapter<BluetoothDevice> initializeListAdapter(Context context, ListAdapterCallback callback) {
        listAdapter = new ArrayAdapter<BluetoothDevice>(context, 0, BluetoothHelper.devices) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                DeviceListItemBinding itemBinding = DeviceListItemBinding.inflate(inflater, parent, false);
                DeviceInfo info = BluetoothHelper.getDeviceInfo(position);
                itemBinding.text1.setText(info.name);
                itemBinding.text2.setText(info.address);
                itemBinding.getRoot().setOnClickListener(v -> {
                    if (callback != null) {
                        callback.handler(info.address);
                    }
                });
                return itemBinding.getRoot();
            }
        };
        return listAdapter;
    }

    public ArrayList<BluetoothDevice> getDevices() {
        return devices;
    }

    public static BluetoothHelper getInstance() {
        initialize();
        return instance;
    }

    public static boolean isAdapterInitialized() {
        return adapter != null;
    }

    public static IntentFilter getScanFilter() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        return filter;
    }

    private BluetoothHelper() {
        // Class must not be instantiated  - use private constructor
    }

    public boolean isAdapterOn() {
        return adapter.getState() == BluetoothAdapter.STATE_ON;
    }

    public BluetoothDevice getDevice(String address) {
        return adapter.getRemoteDevice(address.toUpperCase());
    }

    @SuppressLint("MissingPermission")
    public static DeviceInfo getDeviceInfo(int index) {
        // it is safe to suppress permission,
        // because when this method is call, bluetooth permissions
        // will already be granted.
        if ((index >= 0) && (index < devices.size())) {
            BluetoothDevice device = devices.get(index);
            if (device != null) {
                return new DeviceInfo(device.getName(), device.getAddress());
            }
        }
        return new DeviceInfo("<unknown device>", "<00:00:00:00:00:00>");
    }

    public static void clearDevices() {
        devices.clear();
    }

    @SuppressLint("MissingPermission")
    public static void scanAsync(Context context, BooleanCallback callback) {
        // Don't start scanning until 'readyToScan' is true
        // 'readytoScan' must be set by the receiver (after an event for scan finished hasbeen broadcast)
        // This is important to maintain proper UI state.
        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                if (isReadyToScan()) {
                    boolean started = false;
                    boolean stopped = stopScan(context);
                    if (stopped) {
                        // startDiscovery() will scan for 120 seconds max.
                        started = adapter.startDiscovery();
                    }
                    if (callback != null) {
                        callback.handler(started);
                    }
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                        Log.d("TAG", "scanAsync: waiting for cancel scan event...");
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        });
    }
// I have an appointment in 10 minutes. If you still want to try the last change. You can do. But i will take a shower if that's ok :)
    //yes, that is fine.
    // Actually, let me save and we can do this tomorrow. I will grab a copy of the repo.
    public boolean stopScan(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are checked by caller activity
            return false;
        }
        setReadyToScan(false);
        return adapter.cancelDiscovery();
    }


}