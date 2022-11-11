package de.eloc.eloc_control_panel.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

import de.eloc.eloc_control_panel.databinding.DeviceListItemBinding;

public class BluetoothHelper {
    private static BluetoothAdapter adapter;
    private static BluetoothHelper instance;
    private static ArrayAdapter<BluetoothDevice> listAdapter;
    private static boolean readyToScan = false;

    public static ArrayList<BluetoothDevice> devices = new ArrayList<>();

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

            final Comparator<BluetoothDevice> comparator = (a, b) -> {
                boolean aValid = a.getName() != null && !a.getName().isEmpty();
                boolean bValid = b.getName() != null && !b.getName().isEmpty();
                if (aValid && bValid) {
                    int ret = a.getName().compareTo(b.getName());
                    if (ret != 0) return ret;
                    return a.getAddress().compareTo(b.getAddress());
                }
                if (aValid) return -1;
                if (bValid) return +1;
                return a.getAddress().compareTo(b.getAddress());
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                devices.sort(comparator);
            } else {
                BluetoothDevice[] array = devices.toArray(new BluetoothDevice[]{});
                Arrays.sort(array, comparator);
                devices = new ArrayList<>(List.of(array));
            }
            System.out.println("traceeeeeeeeADDCALLED" + devices.get(0).getName());

            listAdapter.notifyDataSetChanged();
            if (callback != null) {
                callback.handler(devices.isEmpty(), false);
            }
        }
    }

    public static ArrayAdapter<BluetoothDevice> initializeListAdapter(Context context, ListAdapterCallback callback) {
        listAdapter = new ArrayAdapter<>(context, 0, BluetoothHelper.devices) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                DeviceListItemBinding itemBinding = DeviceListItemBinding.inflate(inflater, parent, false);
                DeviceInfo info = getDeviceInfo(position);
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

    public static DeviceInfo getDeviceInfo(int index) {
        if ((index >= 0) && (index < devices.size())) {
            BluetoothDevice device = devices.get(index);
            return DeviceInfo.fromDevice(device);
        }
        return DeviceInfo.getDefault();
    }

    public static void clearDevices() {
        devices.clear();
    }

    @SuppressLint("MissingPermission")
    public static void scanAsync(Context context, BooleanCallback callback) {
        // Don't start scanning until 'readyToScan' is true
        // 'readyToScan' must be set by the receiver
        // (after an event for scan finished hasbeen broadcast) or by stopScan()
        // This is important to maintain proper UI state.
        boolean stopped = stopScan(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            while (true) {
                if (isReadyToScan()) {
                    boolean started = false;
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
    public static boolean stopScan(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are checked by caller activity
            return false;
        }
        setReadyToScan(false);
        boolean stopped = true;
        if (adapter.isDiscovering()) {
            stopped = adapter.cancelDiscovery();
        } else {
            setReadyToScan(true);
        }
        return stopped;
    }


}