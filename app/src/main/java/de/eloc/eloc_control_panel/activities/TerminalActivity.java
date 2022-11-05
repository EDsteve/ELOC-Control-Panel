package de.eloc.eloc_control_panel.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import de.eloc.eloc_control_panel.App;
import de.eloc.eloc_control_panel.OpenLocationCode;
import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.SerialListener;
import de.eloc.eloc_control_panel.SerialService;
import de.eloc.eloc_control_panel.SerialService.SerialBinder;
import de.eloc.eloc_control_panel.SerialSocket;
import de.eloc.eloc_control_panel.SimpleLocation;
import de.eloc.eloc_control_panel.TextUtil;
import de.eloc.eloc_control_panel.helpers.Helper;
import de.eloc.eloc_control_panel.databinding.ActivityTerminalBinding;
import de.eloc.eloc_control_panel.BuildConfig;
import de.eloc.eloc_control_panel.helpers.Helper;

public class TerminalActivity extends AppCompatActivity implements ServiceConnection, SerialListener {
    private ActivityTerminalBinding binding;
    public static final String ARG_DEVICE = "device";
    private boolean refreshing = false;

    private enum Connected {False, Pending, True}

    // TODO: Replace log.i

    private enum DeviceState {
        Recording,
        Stopping,
        Ready,
    }

    final String gVersion = BuildConfig.VERSION_NAME;
    //   final String gVersion = "AppBeta4.1";
    public ActivityResultLauncher<Intent> settingsLauncher;
    private String deviceAddress = "<no address>";
    private SerialService service;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private final boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private final String newline = TextUtil.newline_crlf;
    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;
    private final boolean gInitialSettings = false;
    public Long gLastGoogleSync = 0L; //need to get the elapsedRealtime ()
    public Long gLastTimeDifferenceMillisecond = 0L;
    private String locationCode;
    public float locationAccuracy;
    private DeviceState deviceState = DeviceState.Ready;
    private String recordingTime = "0:00 h";
    private boolean hasSDCardError = false;
    private MenuItem elocSettingsItem;

    public SimpleLocation theLocation;
    public String rangerName;
    private int redColor = Color.WHITE;
    private int greenColor = Color.WHITE;
    private int yellowColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("elocApp", "terminal onCreate");
        binding = ActivityTerminalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent bindIntent = new Intent(this, SerialService.class);
        bindService(bindIntent, this, Context.BIND_AUTO_CREATE);

        setActionBar();
        setListeners();
        setLaunchers();
        setupScrollHack();

        Bundle extras = getIntent().getExtras();
        boolean hasDevice = false;
        if (extras != null) {
            hasDevice = extras.containsKey(ARG_DEVICE);
            deviceAddress = extras.getString(ARG_DEVICE, "<no address found>");
        }

        if (!hasDevice) {
            finish();
            return;
        }

        greenColor = ContextCompat.getColor(this, R.color.on_color);
        redColor = ContextCompat.getColor(this, R.color.off_color);
        yellowColor = ContextCompat.getColor(this, R.color.middle_color);

        SharedPreferences mPrefs = App.getInstance().getSharedPrefs();
        rangerName = mPrefs.getString("rangerName", "notSet");

        Log.i("elocApp", "terminal rangerName " + rangerName);
        Log.i("elocApp", "device address " + deviceAddress);
        getBestTimeEstimate();
        //startLocation();

        receiveText = binding.receiveText;                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        //marker
        appendReceiveText("\nEloc App version: " + gVersion + "\n");

        startLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        binding.refreshLayout.setVisibility(View.GONE);
        binding.infoLayout.setVisibility(View.VISIBLE);

        Log.i("elocApp", "terminal onResume()");
        locationCode = "UNKNOWN";
        locationAccuracy = 99.0f;
        //sendLocation();

        if (theLocation.hasLocationEnabled()) {
            Log.i("elocApp", "gps enabled");

            //appendReceiveText("\nGetting GPS coords\n");

        } else {
            Log.i("elocApp", "gps off");
            //appendReceiveText("\nPlease enable GPS\n");
            //theLocation.openSettings(context);

        }


        //setRecButton();

        // if the recbtn is start recording then wait gps
        theLocation.beginUpdates();
        if (deviceState == DeviceState.Ready) {
            updateRecordButton();
        }

        if (initialStart && service != null) {
            initialStart = false;
        }
        binding.gpsValueTv.setText(R.string.wait);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("elocApp", "terminal onstart()");
        super.onStart();
        if (service != null) {
            service.attach(this);
        } else {
            // prevents service destroy on unbind from recreated activity caused by orientation change
            startService(new Intent(this, SerialService.class));
        }
    }

    @Override
    protected void onStop() {
        Log.i("elocApp", "terminal onstart()");
        handleStop();
        //theLocation.endUpdates();

        if (service != null && !isChangingConfigurations()) {
            service.detach();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i("elocApp", "terminal ondestroy()");
        try {
            unbindService(this);
        } catch (Exception ignored) {
        }
        if (connected != Connected.False)
            disconnect();
        stopService(new Intent(this, SerialService.class));
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_terminal, menu);
        elocSettingsItem = menu.findItem(R.id.elocsettings);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        if (refreshing) {
            Helper.showSnack(binding.coordinator, "Currently unavailable");
            return true;
        } else if (id == R.id.elocsettings) {
           openSettings();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        SerialBinder serialBinder = (SerialBinder) binder;
        if (serialBinder != null) {
            service = serialBinder.getService();
            service.attach(this);
            if (initialStart) {
                initialStart = false;
                runOnUiThread(() -> connect(false));
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        Helper.showSnack(binding.coordinator, getString(R.string.connected));
        connected = Connected.True;
        //send("_setClk_"+getBestTimeEstimate());
        //send("settingsRequest");
    }

    @Override
    public void onSerialConnectError(Exception e) {
        updateDeviceState(DeviceState.Ready, "Connection Lost");
        Helper.showSnack(binding.coordinator, "Connection Lost");
        updateRecordButton();
        // status("connection failed: " + e.getMessage()); // TODO: this message must be in a log
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        updateDeviceState(DeviceState.Ready, "Connection Lost");
        Helper.showSnack(binding.coordinator, "Connection Lost");
        updateRecordButton();
        receiveText.setText("");
        //status("connection lost: " + e.getMessage()); //TODO: This message should be in some kind of log
        disconnect();
    }

    private void openSettings() {
        if (deviceState == DeviceState.Ready) {
            settingsLauncher.launch(new Intent(TerminalActivity.this, MainSettingsActivity.class));
        }
    }

    /*
     * Serial + UI
     */
    private void connect(boolean notify) {

        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            // this line might have introduced a bug. This is bluetooth connection and not recording sttatus.
            //updateDeviceState(DeviceState.Recording, null);
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getApplicationContext(), device);
            service.connect(socket);
            boolean success = service.isConnected();
            if (notify && success) {
                Helper.showSnack(binding.coordinator, getString(R.string.connected));
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        } catch (Exception e) {
            onSerialConnectError(e);
        } finally {
            binding.refreshLayout.setVisibility(View.GONE);
            binding.infoLayout.setVisibility(View.VISIBLE);
            refreshing = false;
        }

    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void setupScrollHack() {
        // Keep swipe gesture for SwipeToRefreshLayout disabled
        // Only enable it when the scrollview is at the top.
        // Do this this avoid the SwipeToRefreshLayout stealing focus
        // and making scrolling broken

        binding.swipeRefreshLayout.setEnabled(false);
        binding.scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                Log.d("TAG", "onScrollChange: " + scrollY);
                if (scrollY <= 0) {
                    binding.swipeRefreshLayout.setEnabled(true);
                } else {
                    binding.swipeRefreshLayout.setEnabled(false);
                }
            }
        });
    }

    private void setActionBar() {
        setSupportActionBar(binding.appbar.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }
    }

    private void setLaunchers() {
        settingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Bundle extras = intent.getExtras();
                        if (extras != null) {
                            String command = extras.getString(MainSettingsActivity.COMMAND, null);
                            if (command != null) {
                                String resultMessage = send(command);
                                if (resultMessage == null) {
                                    resultMessage = "Command sent successfully";
                                }
                                Helper.showSnack(binding.coordinator, resultMessage);
                            }
                        }
                    }
                }
            }
        });
    }

    private void receive(byte[] data) {

        if (hexEnabled) {
            receiveText.append(TextUtil.toHexString(data) + '\n');
        } else {
            String msg = new String(data);


            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
            elocReceive(msg);
        }
    }

    private void elocReceive(String msg) {
        // Save device settings; but we can see that tthe value was actually saved. So maybe the firmware need to do a follow
        // let check the old project.
        if (msg.startsWith("#")) {
            saveSettings(MainSettingsActivity.DATA_KEY, msg);
        } else if (msg.startsWith("please check")) {
            hasSDCardError = true;
            showSDCardError();
        } else if (msg.startsWith("getClk")) {
            send("_setClk_" + getBestTimeEstimate());
        } else {
            setDeviceInfo(msg);
        }
    }

    private void writeToFile(String data) {

        boolean hasInfo = data.contains("!0!");
        if (!hasInfo) {
            return;
        }
        data = data.replace("statusupdate", "----STATUS----")
                .replace("_@b$_", rangerName)
                .replace("!0!", "Device Name:  ")
                .replace("!1!", "Firmware:  ")
                .replace("!2!", "Battery volts:  ")
                .replace("!3!", "Grid ID:  ")                       //Normally file header
                .replace("!4!", "Up Time Since Boot:  ")
                .replace("!5!", "Record Time Since Boot:  ")
                .replace("!6!", "Current Record Time:  ")
                .replace("!7!", "Recording?:  ")
                .replace("!8!", "Bluetooth Record?:  ")
                .replace("!9!", "Sample Rate:  ")
                .replace("!10!", "Seconds Per File:  ")
                .replace("!11!", "SD Card Free GB:  ")
                .replace("!12!", "Microphone Type:  ")
                .replace("!13!", "Microphone Gain:  ")
                .replace("!14!", "Last GPS Location:  ")
                .replace("!15!", "Last GPS Accuracy:  ")
                .replace("!16!", "Session ID:  ");
        SharedPreferences mPrefs = App.getInstance().getSharedPrefs();
        long lastGoogleTimestamp = Long.parseLong(mPrefs.getString("lastGoogleTimestamp", "0"));
        data = data.trim() + "\nApp last time sync:  " + Long.toString(((System.currentTimeMillis() - lastGoogleTimestamp) / 1000l / 60l)) + " min\n";
        data = data + "App Version:  " + gVersion;


        //msg=top+msg;

        //receiveText.setText("");
        //status(msg);
        receiveText.setText(spanWhite(data.trim()));

        receiveText.post(new Runnable() { //always first one fails
            public void run() {
                receiveText.scrollTo(0, 0);
            }
        });

        //String lines[] = msg.split("\\r?\\n");
        String temp = deviceAddress.replace(":", "-");
        String filename = temp + ".txt";

        File test = getFilesDir();
        //getAbsolutePath()
        Log.i("elocApp", "file written   " + test.getAbsolutePath() + filename);
        //Log.i("elocApp", msg);


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private SpannableStringBuilder spanWhite(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.white)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //receiveText.append(spn);
        return (spn);
    }

    private void setDeviceInfo(String msg) {
        // Set data from prefs
        SharedPreferences mPrefs = App.getInstance().getSharedPrefs();
        long lastGoogleTimestamp = Long.parseLong(mPrefs.getString("lastGoogleTimestamp", "0"));
        double millisPerHour = 1000 * 3600;
        double hoursSinceLastSync = (System.currentTimeMillis() - lastGoogleTimestamp) / millisPerHour;
        binding.timeSyncValueTv.setText(String.format(Locale.ENGLISH, "%.2f h", hoursSinceLastSync));
        int syncColor = redColor;
        if (hoursSinceLastSync < 48) {
            syncColor = greenColor;
        }
        binding.timeSyncValueTv.setTextColor(syncColor);


        msg = msg.replace("_@b$_", rangerName);

        writeToFile(msg);

        // Set data from bt device
        String[] lines = getStatusLines(msg);

        String fileHeader = null;
        String sampleRate = null;
        String secondsString = null;
        String micGain = null;
        String micType = null;
        String sessionID = null;
        Integer recON = 0;

        if (lines.length > 0) { // got an update this time
            binding.appversionValueTv.setText(gVersion);
            for (String l : lines) {
                if (l.startsWith("Ranger:")) {
                    String rangerName = l.replace("Ranger:", "").trim();
                    binding.rangerNameTv.setText(rangerName);
                } else if (l.startsWith("!0!")) {
                    String deviceName = l.replace("!0!", "").trim();
                    binding.elocTv.setText(deviceName);
                } else if (l.startsWith("!1!")) {
                    String firmware = l.replace("!1!", "").trim();
                    binding.firmwareValueTv.setText(firmware);

                } else if (l.startsWith("!2!")) {
                    String[] parts = l
                            .replace("!2!", "")
                            .split("#");
                    String batteryLevel = "Unknown";
                    if (parts.length >= 2) {
                        batteryLevel = parts[1].trim().toUpperCase();
                        if (batteryLevel.isEmpty()) {
                            batteryLevel = "OK";
                        }
                    }
                    binding.batteryValueTv.setText(batteryLevel);
                    String tmp = batteryLevel.toLowerCase();
                    int batteryValueColor = yellowColor;
                    if (tmp.contains("low")) {
                        batteryValueColor = redColor;
                    } else if (tmp.contains("full")) {
                        batteryValueColor = greenColor;
                    }
                    binding.batteryValueTv.setTextColor(batteryValueColor);

                    if (parts.length >= 1) {
                        binding.batteryVoltageValueTv.setText(parts[0].trim());
                    }
                } else if (l.startsWith("!3!")) {
                    fileHeader = l.replace("!3!", "").trim();
                    binding.fileHeaderNameValueTv.setText(fileHeader);
                } else if (l.startsWith("!4!")) {
                    String uptime = l.replace("!4!", "").trim();
                    setTime(binding.uptimeValueTv, uptime);
                } else if (l.startsWith("!5!")) {
                    String recordBootTime = l.replace("!5!", "").trim();
                    setTime(binding.recordingBootValueTv, recordBootTime);
                } else if (l.startsWith("!6!")) {
                    recordingTime = l.replace("!6!", "").trim();
                    setRecordingTime();
                } else if (l.startsWith("!7!")) {
                    if (l.contains("1")) {
                        updateDeviceState(DeviceState.Recording, null);
                        recON = 1;
                    } else {
                        updateDeviceState(DeviceState.Ready, null);
                        recON = 0;
                    }
                    updateRecordButton();
                    setRecordingTime();
                } else if (l.startsWith("!8!")) {
                    String recordingTime = l.replace("!8!", "").toUpperCase().trim();
                    boolean hasTime = recordingTime.contains(":") || (!recordingTime.toLowerCase().contains("on"));
                    binding.btRecordingValueTv.setText(recordingTime);
                    //binding.btRecordingValueTv.setTextColor(hasTime ? greenColor : redColor);
                } else if (l.startsWith("!9!")) {
                    sampleRate = l.replace("!9!", "").trim();
                    Double rate = parseDouble(sampleRate);
                    String prettyRate = "Unknown";
                    if (rate != null) {
                        prettyRate = formatNumber(rate / 1000, "KHz");
                    }
                    binding.sampleRateValueTv.setText(prettyRate);
                } else if (l.startsWith("!10!")) {
                    secondsString = l.replace("!10!", "").trim();
                    String hoursString = "Unknown";
                    Double seconds = parseDouble(secondsString);
                    if (seconds != null) {
                        double hours = seconds / 3600;
                        hoursString = formatNumber(hours, "h");
                    }
                    binding.hoursPerFileValueTv.setText(hoursString);
                } else if (l.startsWith("!11!")) {
                    String gb = l.replace("!11!", "").trim();
                    Double usedGB = parseDouble(gb);
                    hasSDCardError = (usedGB == null) || usedGB <= 0;
                    binding.sdCardErrorBtn.setVisibility(hasSDCardError ? View.VISIBLE : View.GONE);
                    binding.sdCardValueTv.setText(String.format("%s GB", gb));
                    binding.sdCardValueTv.setTextColor(usedGB < 40 ? redColor : greenColor);
                } else if (l.startsWith("!12!")) {
                    micType = l.replace("!12!", "").trim();
                    binding.microphoneValueTv.setText(micType);
                } else if (l.startsWith("!13!")) {
                    micGain = l.replace("!13!", "").trim();
                    if ("11".equals(micGain)) {
                        micGain = "HIGH";
                    } else if ("14".equals(micGain)) {
                        micGain = "LOW";
                    }
                    binding.gainValueTv.setText(micGain);
                } else if (l.startsWith("!14!")) {
                    String location = l.replace("!14!", "").trim();
                    binding.lastLocationValueTv.setText(location);
                } else if (l.startsWith("!15!")) {
                    String gpsAccuracyString = l
                            .replace("!15!", "")
                            .replace(" m", "")
                            .trim();
                    Double accuracy = parseDouble(gpsAccuracyString);
                    String prettyAccuracy = "Unknown";
                    if (accuracy != null) {
                        prettyAccuracy = formatNumber(accuracy, "m");
                        if (accuracy < 5) {
                            binding.gpsValueTv.setTextColor(greenColor);
                        } else if (accuracy < 10) {
                            binding.gpsValueTv.setTextColor(yellowColor);
                        } else {
                            binding.gpsValueTv.setTextColor(redColor);
                        }
                    }
                    binding.lastAccuracyValueTv.setText(prettyAccuracy);
                } else if (l.startsWith("!16!")) {
                    if (recON == 0) {
                        sessionID = "";
                        binding.sessionIdValueTv.setText(sessionID);
                    } else {
                        sessionID = l.replace("!16!", "").trim();
                        binding.sessionIdValueTv.setText(sessionID);
                    }
                }
                if ((fileHeader != null) && (sampleRate != null) && (secondsString != null)) {
                    String settings = String.format(
                            Locale.ENGLISH,
                            "#%s#%s#%s",
                            sampleRate, secondsString, fileHeader);
                    saveSettings(MainSettingsActivity.DATA_KEY, settings);
                }
                if ((micGain != null) && (micType != null)) {
                    String settings = String.format(
                            Locale.ENGLISH,
                            "#%s#%s",
                            micType, micGain);
                    saveSettings(MainSettingsActivity.MIC_DATA_KEY, settings);
                }
            }
        }
    }

    private String[] getStatusLines(String data) {
        // Valid update datat starts with 'statusupdate'
        if (data == null) {
            data = "";
        }
        ArrayList<String> lines = new ArrayList<>();
        data = data.trim();
        if (data.startsWith("statusupdate")) {
            String[] parts = data.split("\n");
            for (String s : parts) {
                if (s.startsWith("Ranger") || s.startsWith("!")) {
                    lines.add(s);
                }
            }
        }
        return lines.toArray(new String[]{});
    }

    private Double parseDouble(String rawValue) {
        Double result = null;
        if (rawValue == null) {
            rawValue = "";
        }
        try {
            result = Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException ignore) {
        }
        return result;
    }

    private void setTime(TextView textView, String time) {
        if (time != null && (!time.contains("0.00"))) {
            textView.setText(time.trim());
            textView.setTextColor(greenColor);
        } else {
            textView.setText("OFF");
            textView.setTextColor(redColor);
        }
    }

    private void setRecordingTime() {
        String text;
        int color = redColor;
        if (recordingTime != null && (!recordingTime.contains("0.00"))) {
            text = recordingTime.trim();
            color = greenColor;
        } else if (deviceState == DeviceState.Recording) {
            text = "0:00 h";
            color = greenColor;
        } else {
            text = "OFF";
        }
        binding.recordingValueTv.setText(text);
        binding.recordingValueTv.setTextColor(color);
    }

    private String formatNumber(double number, String units) {
        NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(number) + units;
    }

    private String send(String str) {
        if (connected != Connected.True) {
            return "not connected";
        }
        try {
            String msg;
            byte[] data;
            if (hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            //SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            //spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //receiveText.append(spn); //mark
            appendReceiveText(msg + "\n");
            service.write(data);
            return null;
        } catch (Exception e) {
            onSerialIoError(e);
            return "Command not sent- error occurred!";
        }
    }

    public void appendReceiveText(String stuff) {
        SpannableStringBuilder temp = new SpannableStringBuilder(stuff);
        temp.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, temp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(temp);
    }

    private String getBestTimeEstimate() {
        //returns a string with the following:
        //X_Y_ZZZZZZZZZZZZZZZ

        SharedPreferences mPrefs = App.getInstance().getSharedPrefs();

        //long googletimestamp=  Long.parseLong("0");
        long lastGoogleTimestamp = Long.parseLong(mPrefs.getString("lastGoogleTimestamp", "0"));
        long previouselapsedtime = Long.parseLong(mPrefs.getString("elapsedTimeAtGoogleTimestamp", "0"));
        long currentElapsedTime = SystemClock.elapsedRealtime();
        long elapsedTimeDifferenceMinutes = (currentElapsedTime - previouselapsedtime) / 1000L / 60L;
        long elapsedTimeDifferenceMS = (currentElapsedTime - previouselapsedtime);
        String X, Y, timestamp;

        if (true || (lastGoogleTimestamp == 0L) || (elapsedTimeDifferenceMinutes > (48 * 60))) {
            X = "P";

            Y = Long.toString(elapsedTimeDifferenceMinutes);
            timestamp = X + "__" + Y + "___" + Long.toString(System.currentTimeMillis());
            //Toast toast = Toast.makeText( context, " other thing", 8000);
            //toast.show();
        } else {
            X = "G";
            Y = Long.toString(elapsedTimeDifferenceMinutes);
            timestamp = X + "__" + Y + "___" + Long.toString((lastGoogleTimestamp + elapsedTimeDifferenceMS));
            //Toast toast = Toast.makeText( context, "elapsed time diff: "+Long.toString(elapsedTimeDifferenceMinutes)+" min", 8000);
            //toast.show();
        }
        //Toast toast = Toast.makeText( context, timestamp, 8000);
        //toast.show();
        return (timestamp);
    }

    private void updateDeviceState(DeviceState state, String errorMessage) {
        deviceState = state;
        if (elocSettingsItem != null) {
            elocSettingsItem.setEnabled(false);
        }
        switch (state) {
            case Recording:
                binding.statusTv.setText(R.string.connected);
                binding.statusIcon.setImageResource(R.drawable.connected);
                break;
            case Ready:
                binding.statusTv.setText("Ready");
                binding.statusIcon.setImageBitmap(null);
                if (elocSettingsItem != null) {
                    elocSettingsItem.setEnabled(true);
                }
                break;
            case Stopping:
                binding.statusTv.setText("Please wait...");
                binding.statusIcon.setImageResource(R.drawable.connecting);
                break;
        }
        if (errorMessage != null) {
            binding.statusIcon.setImageResource(R.drawable.error);
            binding.statusTv.setText(errorMessage);
        }
        updateRecordButton();
    }

    private void saveSettings(String key, String settings) {
        SharedPreferences.Editor editor = App.getInstance().getSharedPrefs().edit();
        editor.putString(key, settings);
        editor.apply();
    }

    private void updateRecordButton() {
        int text = R.string.rec_state_ready;
        int color = greenColor;
        switch (deviceState) {
            case Recording:
                text = R.string.rec_state_recording;
                color = redColor; // Red when recording
                break;
            case Stopping:
                text = R.string.rec_state_wait;
                color = yellowColor;
                break;
            case Ready:
                text = R.string.rec_state_ready;
                color = greenColor; // Green when ready
                break;
        }
        binding.recBtn.setText(text);
        binding.recBtn.setBackgroundColor(color);
    }

    private void setListeners() {
        binding.settingsSection.setOnClickListener(view -> openSettings());
        binding.sdCardValueTv.setOnClickListener(view -> showSDCardError());
        binding.sdCardErrorBtn.setOnClickListener(view -> showSDCardError());
        binding.recBtn.setOnClickListener(view -> recordButtonClicked());
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        binding.instructionsButton.setOnClickListener(view -> {
            Helper.openInstructionsUrl(TerminalActivity.this);
        });
    }

    private void refresh() {

        binding.refreshLayout.setVisibility(View.VISIBLE);
        binding.infoLayout.setVisibility(View.GONE);
        refreshing = true;
        disconnect();
        connect(true);
    }

    private void recordButtonClicked() {
        switch (deviceState) {
            case Recording:
                updateDeviceState(DeviceState.Stopping, null);
                updateRecordButton();
                binding.recBtn.setBackgroundColor(0x000000);
                send("stoprecord"); //When this line is executed and command is sent, we are supposed to receive a message from the device toindicate when device is disconnected, right?
                // TODO: Notes for future: remove line below and wait for update from !7! value.
                updateDeviceState(DeviceState.Ready, null);
                break;
            case Ready:
                if (hasSDCardError) {
                    showSDCardError();
                    return;
                }
                if (locationAccuracy <= 8.1) {
                    startRecording();
                } else {
                    popUpRecord();
                }
                break;
            default:
                break;
        }
    }

    private void startRecording() {
        binding.recBtn.setText("please wait...");
        send("setGPS^" + locationCode + "#" + Float.toString(locationAccuracy));
        //sendDelayed("_record_",1700);
        handleStop();
    }

    private void handleStop() {
        locationCode = "UNKNOWN";
        locationAccuracy = 99.0f;
        theLocation.endUpdates();
    }

    private void showSDCardError() {
        if (hasSDCardError) {
            Helper.showAlert(this, "Check SD card!");
        }
    }

    private void popUpRecord() {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage("Please wait for better GPS accuracy < 8 m")
                .setPositiveButton("Record Anyway", (dialog, which) -> {
                    // do something here on OK
                    Log.i("elocApp", "clicked record");
                    dialog.cancel();
                    startRecording();
                })
                .setNegativeButton("Wait    ", (dialog, which) -> {
                    dialog.cancel();
                    Log.i("elocApp", "clicked wait");
                })
                .show();
    }

    private void startLocation() {
        //It really depends on the way you call that constructor. Make sure not to enable passive mode while using a network/coarse location.
        //Passive mode is only available for GPS/fine location.
        boolean requireFineGranularity = true;
        boolean passiveMode = false;
        long updateIntervalInMilliseconds = 3 * 1000; //10 secs?
        boolean requireNewLocation = true;
        theLocation = new SimpleLocation(this, requireFineGranularity, passiveMode, updateIntervalInMilliseconds, requireNewLocation);
        //Button gpsBtn = findViewById(R.id.gpsBtn);

        theLocation.setBlurRadius(0);
        theLocation.setListener(new SimpleLocation.Listener() {
            //OpenLocationCode(double latitude, double longitude, int codeLength);
            OpenLocationCode theCode;

            public void onPositionChanged() {
                //Log.i("elocApp", "position changed");
                //my house balcony +- 2m in locus is 6MJWMRHV+9Q
                locationAccuracy = theLocation.getAccuracy();

                theCode = new OpenLocationCode(theLocation.getLatitude(), theLocation.getLongitude());
                locationCode = theCode.getCode();
                Log.i("elocApp", "code: " + locationCode + "  lat: " + Double.toString(theLocation.getLatitude()) + "   lon: " + Double.toString(theLocation.getLongitude()) + "   alt: " + Double.toString(theLocation.getAltitude()) + "   acc: " + Float.toString(locationAccuracy));
                String prettyAccuracy = formatNumber(locationAccuracy, "m");
                if (locationAccuracy < 8) {
                    binding.gpsValueTv.setTextColor(greenColor);
                } else if (locationAccuracy < 12) {
                    binding.gpsValueTv.setTextColor(yellowColor);
                } else {
                    binding.gpsValueTv.setTextColor(redColor);
                }

                binding.gpsValueTv.setText(prettyAccuracy);

                // if (recBtn.getText().toString().startsWith("START RECORDING")) recBtn.setBackgroundColor(0xFFFF0000);
                if (locationAccuracy < 12.0) {


                }

            }

        });

        //theLocation.beginUpdates();

    }
}