package de.eloc.eloc_control_panel.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.eloc.eloc_control_panel.BuildConfig;
import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.SNTPClient;
import de.eloc.eloc_control_panel.UploadFileAsync;
import de.eloc.eloc_control_panel.databinding.ActivityMainBinding;
import de.eloc.eloc_control_panel.databinding.PopupWindowBinding;
import de.eloc.eloc_control_panel.helpers.BluetoothHelper;
import de.eloc.eloc_control_panel.helpers.Helper;
import de.eloc.eloc_control_panel.ng.models.AppPreferenceManager;
import de.eloc.eloc_control_panel.receivers.BluetoothScanReceiver;
import de.eloc.eloc_control_panel.ng.models.Constants;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    public String rangerName;
    public boolean gUploadEnabled = false;
    private Long gLastTimeDifferenceMillisecond = 0L;
    private final BluetoothScanReceiver receiver = new BluetoothScanReceiver(this::onListUpdated);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setActionBar();
        initialize();
        Log.i("elocApp", "\n\n\n mainActivity onCreate");


    }

    @Override
    public void onResume() {
        super.onResume();
        checkRangerName();
        registerScanReceiver();

        boolean hasNoDevices = BluetoothHelper.hasEmptyAdapter();
        onListUpdated(hasNoDevices, false);
        if (hasNoDevices) {
            setBluetoothStatus(BluetoothHelper.getInstance().isAdapterOn());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterScanReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_devices, menu);
        MenuItem aboutItem = menu.findItem(R.id.about);
        if (aboutItem != null) {
            aboutItem.setTitle(BuildConfig.VERSION_NAME);
        }
        if (BluetoothHelper.isAdapterInitialized()) {
            menu.findItem(R.id.bt_settings).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.browseStatusUpdates) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://128.199.206.198/ELOC/files/?C=M;O=D"));
            startActivity(browserIntent);
            return true;
        } else if (id == R.id.timeSync) {
            doSync(5000, true);
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

    private void onListUpdated(boolean isEmpty, boolean scanFinished) {
        boolean showScanUI = isEmpty;
        if (scanFinished) {
            showScanUI = false;
        }
        if (showScanUI) {
            binding.devicesListView.setVisibility(View.GONE);
            binding.initLayout.setVisibility(View.VISIBLE);
            binding.uploadElocStatusButton.setVisibility(View.GONE);
            binding.refreshListButton.setVisibility(View.GONE);
        } else {
            binding.devicesListView.setVisibility(View.VISIBLE);
            binding.initLayout.setVisibility(View.GONE);
            binding.uploadElocStatusButton.setVisibility(View.VISIBLE);
            binding.refreshListButton.setVisibility(View.VISIBLE);
        }
        if (scanFinished) {
            if (BluetoothHelper.hasEmptyAdapter()) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Scan results")
                        .setMessage("No devices were found!")
                        .setNegativeButton(android.R.string.ok, (dialog, i) -> dialog.dismiss())
                        .show();
            }
        }
    }

    private void startScan() {
        // Important: see registerScanReceiver() for notes.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            Context context = MainActivity.this;
            BluetoothHelper.scanAsync(context, MainActivity.this::scanStarted);
        }, 1000);
    }

    private void scanStarted(boolean success) {
        if (!success) {
            Helper.showAlert(this, getString(R.string.scan_error));
            BluetoothHelper.stopScan(this);
        }
    }

    private void setBluetoothStatus(boolean isOn) {
        String statusMessage = "<bluetooth is disabled>";
        if (isOn) {
            statusMessage = "<scanning for eloc devices>";
            startScan();
        }
        binding.status.setText(statusMessage);
    }

    private void uploadElocStatus() {
        //item.setEnabled(false);
        // no use zipFileAtPath instead
        //concatenate files and send one.
        //FileOutputStream fOut = openFileOutput("savedData.txt");
        try {
            //check if has internet connection first.
            File[] files = getFilesDir().listFiles();
            //String filename;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            loadRangerName();
            String filestring = "update " + sdf.format(new Date()) + ".upd";
            //String tempfilename = "tempfilename.upd";
            //start out with a temp file
            OutputStreamWriter fileout = new OutputStreamWriter(openFileOutput(filestring, Context.MODE_PRIVATE));
            int filecounter = 0;
            if (files != null) {
                //File outputfile;
                for (File file : files) {
                    if (!file.isDirectory()) {
                        if (file.getName().endsWith(".txt")) {
                            filecounter++;
                            Log.i("elocApp", "writing file   " + file.getName());
                            fileout.write(fileToString(file) + "\n\n\n");
                        }
                    }
                }

                fileout.write("\n\n\n end of updates");
                fileout.close();
                if (filecounter > 0) {
                    String filename = getFilesDir().getAbsolutePath() + "/" + filestring;
                    Log.i("elocApp", "uploading   " + filename);
                    UploadFileAsync.run(filename, getFilesDir(), this::showSnack);
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
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("elocApp", "devices onstop()");
        BluetoothHelper.getInstance().stopScan(this);
    }


    private void setupListView() {
        ArrayAdapter<BluetoothDevice> adapter = BluetoothHelper.initializeListAdapter(this, this::showDevice);
        binding.devicesListView.setAdapter(adapter);
    }

    private void checkRangerName() {
        loadRangerName();
        rangerName = rangerName.trim();
        if (TextUtils.isEmpty(rangerName) || rangerName.equals(Constants.DEFAULT_RANGER_NAME)) {
            editRangerName();
        }
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
        BluetoothHelper.getInstance().stopScan(this);

        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra(TerminalActivity.ARG_DEVICE, address);
        startActivity(intent);
    }


/* todo: this method does not do anythis except clear list.
    private void refresh() {
        listItems.clear();
//        if(bluetoothAdapter != null) {
//             for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
//             if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
//             listItems.add(device);
//        }
        Collections.sort(listItems, MainActivity::compareTo);
        listAdapter.notifyDataSetChanged();
    }*/

    private void setListeners() {
        binding.instructionsButton.setOnClickListener(view -> Helper.openInstructionsUrl(MainActivity.this));
        binding.refreshListButton.setOnClickListener(view -> startScan());
        binding.uploadElocStatusButton.setOnClickListener(view -> uploadElocStatus());
    }

    private void initialize() {
        setListeners();
        setupListView();
        listFiles();
        loadRangerName();
    }

    private void registerScanReceiver() {
        IntentFilter filter = BluetoothHelper.getScanFilter();
        registerReceiver(receiver, filter);

        // Devices appear to be found after there is a brief delay..
        // Registration of receiver possible causes some kind of delay
        // or is possibly async?? More research needed. for now let's
        // have startScan() wait for 1 second before actually scanning.
        // be sure to apply the delay in startScan()
    }

    private void unregisterScanReceiver() {
        unregisterReceiver(receiver);
    }

    private void loadRangerName() {
        rangerName = AppPreferenceManager.INSTANCE.getRangerName();
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

        }
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
        popupWindowBinding.rangerName.setText(rangerName);
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Input Your Ranger ID")
                .setView(popupWindowBinding.getRoot())
                .setPositiveButton("SAVE", (dialog, which) -> {
                    // do something here on OK
                    Editable editable = popupWindowBinding.rangerName.getText();
                    if (editable != null) {
                        dialog.dismiss();
                        String name = editable.toString().trim();
                        validateRangerName(name);
                    }
                })
                .show();
    }

    private void validateRangerName(String name) {
        if (name == null) {
            name = "";
        }
        name = name.trim();
        saveRangerName(name);
        if (TextUtils.isEmpty(name)) {
            checkRangerName();
        } else {
            Log.i("elocApp", "new ranger name is " + name);
        }
        Helper.hideKeyboard(this);
    }

    private void setActionBar() {
        setSupportActionBar(binding.appbar.toolbar);
    }

    private void saveRangerName(String theName) {
        AppPreferenceManager.INSTANCE.setRangerName(theName);
        loadRangerName();
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
                AppPreferenceManager.INSTANCE.saveTimestamps(SystemClock.elapsedRealtime(), googletimestamp);
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

}
