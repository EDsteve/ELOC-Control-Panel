package de.eloc.eloc_control_panel.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import de.eloc.eloc_control_panel.App;
import de.eloc.eloc_control_panel.BuildConfig;
import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.SNTPClient;
import de.eloc.eloc_control_panel.UploadFileAsync;
import de.eloc.eloc_control_panel.databinding.ActivityMainBinding;
import de.eloc.eloc_control_panel.databinding.DeviceListItemBinding;
import de.eloc.eloc_control_panel.databinding.PopupWindowBinding;

public class MainActivity extends AppCompatActivity {
//public static long testme=0L;

/* 	Context context = this;
	boolean requireFineGranularity = false;
	boolean passiveMode = false;
	long updateIntervalInMilliseconds = 10 * 60 * 1000;
	boolean requireNewLocation = false;
	new SimpleLocation(context, requireFineGranularity, passiveMode, updateIntervalInMilliseconds, requireNewLocation);
	 */

    private ActivityMainBinding binding;
    private BluetoothAdapter bluetoothAdapter;
    private static MainActivity instance;
    public String rangerName;
    private Menu menu;
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    public boolean gUploadEnabled = false;
    private Long gLastTimeDifferenceMillisecond = 0L;
    private long gLastGoogleTimeSyncMS = 0L;
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Create a BroadcastReceiver for ACTION_FOUND when scanning for bt devices.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Toast.makeText(getActivity(), "in onreceive ", Toast.LENGTH_LONG).show();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                //Toast.makeText(getActivity(), "ACTION_FOUND", Toast.LENGTH_LONG).show();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();
//                String deviceHardwareAddress = device.getAddress(); // MAC address

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) { //not working on android 7
                    //Toast.makeText(getActivity(), "BOND_BONDED", Toast.LENGTH_LONG).show();
                    addDevice(device);
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    //Toast.makeText(getActivity(), "BOND_NONE", Toast.LENGTH_LONG).show();

                    try {
                        if (deviceName.toLowerCase().startsWith("eloc")) {
                            addDevice(device);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                device.createBond();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("elocApp", "I got an error", e);
                    }
                }
                if (!listItems.isEmpty()) {
                    binding.devicesListView.setVisibility(View.VISIBLE);
                    binding.initLayout.setVisibility(View.GONE);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setActionBar();
        initialize();
        Log.i("elocApp", "\n\n\n mainActivity onCreate");
        instance = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_devices, menu);
        MenuItem aboutItem = menu.findItem(R.id.about);
        if (aboutItem != null) {
            aboutItem.setTitle(BuildConfig.VERSION_NAME);
        }
        if (bluetoothAdapter == null) {
            menu.findItem(R.id.bt_settings).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.rescan) {
            doScan();
            return true;
        } else if (id == R.id.browseStatusUpdates) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://indodic.com/tom/eloc/files/?C=M;O=D"));
            startActivity(browserIntent);
            return true;
        } else if (id == R.id.timeSync) {
            doSync(5000, true);
            return true;
        } else if (id == R.id.uploadeloc) {
            //item.setEnabled(false);
            // no use zipFileAtPath instead
            //concatenate files and send one.
            //FileOutputStream fOut = openFileOutput("savedData.txt");
            try {
                //check if has internet connection first.
                File[] files = getFilesDir().listFiles();
                //String filename;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                rangerName = App.getInstance().getSharedPrefs().getString("rangerName", "notSet");
                String filestring = "update " + sdf.format(new Date()) + ".upd";
                //String tempfilename = "tempfilename.upd";
                //start out with a temp file
                OutputStreamWriter fileout = new OutputStreamWriter(openFileOutput(filestring, Context.MODE_PRIVATE));
                int filecounter = 0;
                if (files != null) {
                    //File outputfile;
                    for (File file : files) {
                        if (file.isDirectory()) {
                            //traverse(file);
                        } else {
                            //mark
                            //temp=fileToString(file);

                            if (file.getName().endsWith(".txt")) {
                                filecounter++;
                                Log.i("elocApp", "writing file   " + file.getName());
                                fileout.write(fileToString(file) + "\n\n\n");
                                //file.delete();
                            }
                        }
                    }

                    fileout.write("\n\n\n end of updates");
                    fileout.close();
                    if (filecounter > 0) {
                        //rename the file
                        //handle the case for multiple upd files.


                        UploadFileAsync upload = new UploadFileAsync();
                        String  filename = getFilesDir().getAbsolutePath() + "/" + filestring;
                        Log.i("elocApp", "uploading   " + filename);
                        upload.run(filename, getFilesDir(), this::showSnack);
                    } else {
//                        File temp = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + filestring);
//                        temp.delete();
                        // not getting deleted.
                        //deleteAllWithExtension(".upd");
                        //fileout.delete();
                        Log.i("elocApp", "Nothing to upload!   ");
                        Helper.showSnack(binding.coordinator, "Nothing to Upload!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("elocApp", "I got an error", e);
            }
            return true;
        }

        if (id == R.id.setRangerName) {
            editRangerName();
        } else if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else {
            return true;
        }
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("elocApp", "devices onstop()");
        bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("elocApp", "devices onresume()");
        String statusMessage;

        binding.initLayout.setVisibility(View.VISIBLE);
        binding.devicesListView.setVisibility(View.GONE);
        if (bluetoothAdapter == null) {
            statusMessage = "<bluetooth not supported>";
        } else if (!bluetoothAdapter.isEnabled()) {
            statusMessage = "<bluetooth is disabled>";
        } else {
            statusMessage = "<scanning for eloc devices>";
        }
        binding.status.setText(statusMessage);
        doScan();
        refresh();
    }

    private void setupListView() {
        listAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, listItems) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                DeviceListItemBinding itemBinding = DeviceListItemBinding.inflate(inflater, parent, false);
                BluetoothDevice device = listItems.get(position);
                itemBinding.text1.setText(device.getName());
                itemBinding.text2.setText(device.getAddress());
                itemBinding.getRoot().setOnClickListener(v -> showDevice(device.getAddress()));
                return itemBinding.getRoot();
            }
        };
        binding.devicesListView.setAdapter(listAdapter);
    }

    private void showSnack(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String msg = message;
                if (msg == null) {
                    msg = "";
                }
                if (msg.trim().isEmpty()) {
                    return;
                }
                Helper.showSnack(binding.coordinator, msg);
            }
        });

    }

    public void showDevice(String address) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //newtom
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.cancelDiscovery();


        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra("device", address);
        startActivity(intent);

    }

    private void addDevice(BluetoothDevice device) {
        if (!listItems.contains(device)) {
            listItems.add(device);
            listAdapter.notifyDataSetChanged();
        }
    }

    private void refresh() {
        listItems.clear();
//        if(bluetoothAdapter != null) {
//             for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
//             if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
//             listItems.add(device);
//        }
        Collections.sort(listItems, MainActivity::compareTo);
        listAdapter.notifyDataSetChanged();
    }

    static Context getContext() {
        return instance;
    }

    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        Context context = getContext();
        if (context != null) {
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
        }
        return -1;
    }

    private void initialize() {

        setupListView();
        listFiles();
        setRangerName();

        boolean hasBluetooth = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (hasBluetooth) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter != null) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);

            //newtom
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            //Immediately after checking (and possibly canceling) discovery-mode, start discovery by calling,

            bluetoothAdapter.startDiscovery();
            //endtom
            //doScan();
        }
    }

    private void setRangerName() {
        rangerName = App.getInstance().getSharedPrefs().getString("rangerName", "notSet");
//            if (rangerName.equals("notSet")) {
//                popUpEditText();
//            }
        Log.i("elocApp", "ranger Name " + rangerName);
    }

    private String fileToString(File file) {
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException ignore) {
            //You'll need to add proper error handling here
        }

        //Find the view by its id
        //TextView tv = (TextView)findViewById(R.id.text_view);
        //Set the text
        return text.toString();
    }

    public void listFiles() {
        Log.i("elocApp", "listing files ");
        File[] files = getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                Log.i("elocApp", "filename " + file.getName());
                //files[i].delete();
            }
        }
    }

    private void editRangerName() {
        PopupWindowBinding popupWindowBinding = PopupWindowBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Input Your Ranger ID")
                .setView(popupWindowBinding.getRoot())
                .setPositiveButton("OK", (dialog, which) -> {
                    // do something here on OK
                    Editable editable = popupWindowBinding.rangerName.getText();
                    if (editable != null) {
                        String name = editable.toString().trim();
                        Log.i("elocApp", "new ranger name is " + name);
                        saveRangerName(name);
                    }
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void setActionBar() {
        setSupportActionBar(binding.appbar.toolbar);
    }

    private void doScan() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
        //newtom
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        //Immediately after checking (and possibly canceling) discovery-mode, start discovery by calling,
        bluetoothAdapter.startDiscovery();
    }

    private void saveRangerName(String theName) {
        SharedPreferences.Editor mEditor = App.getInstance().getSharedPrefs().edit();
        mEditor.putString("rangerName", theName).apply();
    }

    public void doSync(int timeoutMS, boolean showMessage) {
        //send("doing sync");
        SNTPClient.getDate(Calendar.getInstance().getTimeZone(), (rawDate, date, googletimestamp, ex) -> {

            if (googletimestamp == 0) {

                gUploadEnabled = false;
                invalidateOptionsMenu();
                Log.i("elocApp", "google sync failed");

                if (showMessage) {
                    Helper.showSnack(binding.coordinator, "sync FAILED\nCheck internet connection");
                }
            } else {
                gLastTimeDifferenceMillisecond = System.currentTimeMillis() - googletimestamp;
                saveTimestamps(SystemClock.elapsedRealtime(), googletimestamp);
                gLastGoogleTimeSyncMS = System.currentTimeMillis();
                gUploadEnabled = true;
                invalidateOptionsMenu();
                Log.i("elocApp", "google sync success");
                if (showMessage) {
                    String message = getString(R.string.sync_template, gLastTimeDifferenceMillisecond);
                    Helper.showSnack(binding.coordinator, message);
                }
            }

        }, timeoutMS);
        //send("testing latency");
    }


    public void saveTimestamps(Long gCurrentElapsedTimeMS, Long gLastGoogleSyncTimestampMS) {
        SharedPreferences.Editor mEditor = App.getInstance().getSharedPrefs().edit();
        mEditor.putString("elapsedTimeAtGoogleTimestamp", gCurrentElapsedTimeMS.toString()).apply();
        mEditor.putString("lastGoogleTimestamp", gLastGoogleSyncTimestampMS.toString()).apply();
    }


}
