package de.eloc.eloc_control_panel;

import android.app.AlertDialog;
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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.ListFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class DevicesFragment extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    public String rangerName;
    private Menu menu;
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    public boolean gUploadEnabled = false;
    public String gVersion = "AppBeta3.2";
    private Long gLastTimeDifferenceMillisecond = 0L;
    private long gLastGoogleTimeSyncMS = 0L;

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
                    listItems.add(device);
                    listAdapter.notifyDataSetChanged();
                }
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    //Toast.makeText(getActivity(), "BOND_NONE", Toast.LENGTH_LONG).show();

                    try {
                        if (deviceName.toLowerCase().startsWith("eloc")) {
                            listItems.add(device);
                            listAdapter.notifyDataSetChanged();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                device.createBond();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("elocApp", "I got an error", e);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("elocApp", "devices oncreate()");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        listFiles();
        setRangerName();

        FragmentActivity activity = getActivity();
        if (activity != null) {
            boolean hasBluetooth = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
            if (hasBluetooth) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
        }
        if (bluetoothAdapter != null) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(receiver, filter);

            //newtom
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            //Immediately after checking (and possibly canceling) discovery-mode, start discovery by calling,

            bluetoothAdapter.startDiscovery();
            //endtom
            //doScan();
        }
        setListAdapter();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_devices, menu);
        if (bluetoothAdapter == null) {
            menu.findItem(R.id.bt_settings).setEnabled(false);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.about).setTitle(gVersion);
//        this.menu = menu;
//        if (gUploadEnabled) {
//            menu.findItem(R.id.uploadeloc).setEnabled(true);
//            menu.findItem(R.id.uploadeloc).setTitle("Upload Eloc Status");
//        } else {
//            menu.findItem(R.id.uploadeloc).setEnabled(false);
//            menu.findItem(R.id.uploadeloc).setTitle("check internet");
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        FragmentActivity activity = getActivity();
        if (activity != null) {
            View header = activity.getLayoutInflater().inflate(R.layout.device_list_header, null, false);
            getListView().addHeaderView(header, null, false);
            setEmptyText("initializing...");
            ((TextView) getListView().getEmptyView()).setTextSize(18);
            setListAdapter(listAdapter);
        }
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
        if (bluetoothAdapter == null) {
            setEmptyText("<bluetooth not supported>");
        } else if (!bluetoothAdapter.isEnabled()) {
            setEmptyText("<bluetooth is disabled>");
        } else {
            setEmptyText("<scanning for eloc devices>");
        }
        //doSync(3000,false);
        doScan();
        refresh();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //newtom
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = listItems.get(position - 1);
        FragmentManager manager = getFragmentManager();
        if (manager != null) {
            Bundle args = new Bundle();
            args.putString("device", device.getAddress());
            Fragment fragment = new TerminalFragment();
            fragment.setArguments(args);
            manager.beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.rescan) {
            doScan();
            return true;
        }

        if (id == R.id.browseStatusUpdates) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://indodic.com/tom/eloc/files/?C=M;O=D"));
            startActivity(browserIntent);
            return true;
        }

        if (id == R.id.timeSync) {
            doSync(5000, true);
            return true;
        }
        FragmentActivity activity = getActivity();
        if ((activity != null) && (id == R.id.uploadeloc)) {
            //item.setEnabled(false);
            // no use zipFileAtPath instead

            // check when last google sync was.
            UploadFileAsync upload = new UploadFileAsync();
            upload.theContext = activity;

            //concatenate files and send one.
            //FileOutputStream fOut = openFileOutput("savedData.txt");
            try {
                //check if has internet connection first.
                File[] files = activity.getFilesDir().listFiles();
                //String filename;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                SharedPreferences mPrefs = activity.getSharedPreferences("label", 0);
                rangerName = mPrefs.getString("rangerName", "notSet");
                String filestring = "update " + sdf.format(new Date()) + ".upd";
                //String tempfilename = "tempfilename.upd";
                //start out with a temp file
                OutputStreamWriter fileout = new OutputStreamWriter(activity.openFileOutput(filestring, Context.MODE_PRIVATE));
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

                        upload.filename = activity.getFilesDir().getAbsolutePath() + "/" + filestring;
                        //upload.filename
                        Log.i("elocApp", "uploading   " + upload.filename);
                        upload.execute(""); //test when this finished?
                    } else {
//                        File temp = new File(getActivity().getFilesDir().getAbsolutePath() + "/" + filestring);
//                        temp.delete();
                        // not getting deleted.
                        //deleteAllWithExtension(".upd");
                        //fileout.delete();
                        Log.i("elocApp", "Nothing to upload!   ");
                        Toast.makeText(activity, "Nothing to Upload!", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("elocApp", "I got an error", e);
            }
            return true;
        }

        if (id == R.id.setRangerName) {
            popUpEditText();

            // inflate the layout of the popup window
            View popupView = LayoutInflater.from(activity).inflate(R.layout.popup_window, null);

            // create the popup window
            int width = LinearLayout.LayoutParams.WRAP_CONTENT;
            int height = LinearLayout.LayoutParams.WRAP_CONTENT;
            boolean focusable = true; // lets taps outside the popup also dismiss it
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
            popupWindow.setFocusable(true);
            // show the popup window
            // which view you pass in doesn't matter, it is only used for the window tolken
            popupWindow.showAtLocation(getView(), Gravity.CENTER, 0, 0);

            // dismiss the popup window when touched
            popupView.setOnTouchListener((v, event) -> {
                v.performClick();
                //popupWindow.dismiss();
                return true;
            });
        }

        if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void refresh() {
        listItems.clear();
//        if(bluetoothAdapter != null) {
//             for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
//             if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
//             listItems.add(device);
//        }
        Collections.sort(listItems, DevicesFragment::compareTo);
        listAdapter.notifyDataSetChanged();
    }

    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
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

    private void setListAdapter() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            listAdapter = new ArrayAdapter<BluetoothDevice>(activity, 0, listItems) {
                @NonNull
                @Override
                public View getView(int position, View view, @NonNull ViewGroup parent) {
                    BluetoothDevice device = listItems.get(position);
                    if (view == null) {
                        view = activity.getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                    }
                    TextView text1 = view.findViewById(R.id.text1);
                    TextView text2 = view.findViewById(R.id.text2);
                    text1.setText(device.getName());
                    text2.setText(device.getAddress());
                    return view;
                }
            };
        }
    }

    private void setRangerName() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            SharedPreferences mPrefs = activity.getSharedPreferences("label", 0);
            rangerName = mPrefs.getString("rangerName", "notSet");
//            if (rangerName.equals("notSet")) {
//                popUpEditText();
//            }
        }
        Log.i("elocApp", "ranger Name " + rangerName);
    }

    public void listFiles() {
        Log.i("elocApp", "listing files ");
        FragmentActivity activity = getActivity();
        if (activity != null) {
            File[] files = activity.getFilesDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    Log.i("elocApp", "filename " + file.getName());
                    //files[i].delete();
                }
            }
        }
    }

    private void popUpEditText() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Input Your Ranger ID");

        final EditText input = new EditText(getActivity());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            // do something here on OK
            Log.i("elocApp", "new ranger name is " + input.getText());
            String temp = input.getText().toString().trim();
            saveRangerName(temp);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveRangerName(String theName) {
        Context context = getActivity();
        if (context != null) {
            SharedPreferences mPrefs = context.getSharedPreferences("label", 0);
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putString("rangerName", theName).apply();
        }
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

    public void doSync(int timeoutMS, boolean showToast) {
        //send("doing sync");
        SNTPClient.getDate(Calendar.getInstance().getTimeZone(), (rawDate, date, googletimestamp, ex) -> {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                if (googletimestamp == 0) {

                    gUploadEnabled = false;
                    activity.invalidateOptionsMenu();
                    Log.i("elocApp", "google sync failed");

                    if (showToast) {
                        Toast toast = Toast.makeText(activity, "sync FAILED\nCheck internet connection", Toast.LENGTH_LONG);
                        toast.show();
                    }
                } else {
                    gLastTimeDifferenceMillisecond = System.currentTimeMillis() - googletimestamp;
                    saveTimestamps(SystemClock.elapsedRealtime(), googletimestamp);
                    gLastGoogleTimeSyncMS = System.currentTimeMillis();
                    gUploadEnabled = true;
                    activity.invalidateOptionsMenu();
                    Log.i("elocApp", "google sync success");
                    if (showToast) {
                        Toast toast = Toast.makeText(activity, "sync SUCCESS\n Your phone differs by  " + gLastTimeDifferenceMillisecond.toString() + " ms", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            }
        }, timeoutMS);
        //send("testing latency");
    }

    public void saveTimestamps(Long gCurrentElapsedTimeMS, Long gLastGoogleSyncTimestampMS) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            SharedPreferences mPrefs = activity.getSharedPreferences("label", 0);
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putString("elapsedTimeAtGoogleTimestamp", gCurrentElapsedTimeMS.toString()).apply();
            mEditor.putString("lastGoogleTimestamp", gLastGoogleSyncTimestampMS.toString()).apply();
        }
    }
}
