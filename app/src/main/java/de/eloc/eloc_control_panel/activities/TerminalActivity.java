package de.eloc.eloc_control_panel.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.eloc.eloc_control_panel.SerialService;
import de.eloc.eloc_control_panel.SerialService.SerialBinder;
import de.eloc.eloc_control_panel.TextUtil;
import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.ng3.App;
import de.eloc.eloc_control_panel.ng3.activities.DeviceSettingsActivity;
import de.eloc.eloc_control_panel.databinding.ActivityTerminalBinding;
import de.eloc.eloc_control_panel.ng3.DeviceDriver;
import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity;
import de.eloc.eloc_control_panel.ng3.data.ConnectionStatus;
import de.eloc.eloc_control_panel.ng3.data.PreferencesHelper;
import de.eloc.eloc_control_panel.ng3.interfaces.SocketListener;

public class TerminalActivity extends ThemableActivity /*implements ServiceConnection, SocketListener*/ {
    private final PreferencesHelper preferencesHelper = PreferencesHelper.Companion.getInstance();
    private ActivityTerminalBinding binding;

    public static final String EXTRA_RANGER_NAME = "ranger_name";
    private final double MINUTE = 1.0 / 60; // 1 minute in hrs.
    private final int UPDATE_INTERVAL_MILLIS = 60 * 1000; // Update after each minute.
    private final ExecutorService timeMonitor = Executors.newSingleThreadExecutor();
    private boolean runTimeMonitor = false;


    private String deviceAddress = "";

    private SerialService service;
    private ConnectionStatus connected = ConnectionStatus.Inactive;
    private boolean initialStart = true;
    private final boolean hexEnabled = false;
    private final String newline = TextUtil.newline_crlf;






    public String rangerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTerminalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
      /*dele  getBestTimeEstimate(); */
    }


    @Override
    protected void onResume() {
        super.onResume();

/*dele


        if (initialStart && service != null) {
            initialStart = false;
        }

        runTimeMonitor = true;
        timeMonitor.execute(() -> {
            while (runTimeMonitor) {
                try {
                    Thread.sleep(UPDATE_INTERVAL_MILLIS);
                } catch (InterruptedException ignore) {

                }
                runOnUiThread(() -> {
                    updateRecordingTime();
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        runTimeMonitor = false;
    }

    private void updateRecordingTime() {
        recordingTime += MINUTE;
        setRecordingTime();
    }

    @Override
    protected void onStop() {
        if (service != null && !isChangingConfigurations()) {
            service.detach();
        }
        super.onStop();
        DeviceDriver.INSTANCE.disconnect();
    }

    @Override
    protected void onDestroy() {
        try {
            unbindService(this);
        } catch (Exception ignored) {
        }
        if (connected != ConnectionStatus.Inactive)
            disconnect();
        stopService(new Intent(this, SerialService.class));
        super.onDestroy();
    }

/*dele
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


    /*
     * SerialListener
     */
 /*dele   @Override
    public void onConnect() {
        ActivityHelper.INSTANCE.showSnack(binding.coordinator, getString(R.string.connected));
        connected = ConnectionStatus.Active;

    }

    @Override
    public void onConnectionError(Exception e) {
        updateDeviceState(DeviceState.Ready, "Connection Lost");
        ActivityHelper.INSTANCE.showSnack(binding.coordinator, "Connection Lost");
        // status("connection failed: " + e.getMessage()); // TODO: this message must be in a log
        disconnect();
    }

    @Override
    public void onRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onIOError(Exception e) {
        updateDeviceState(DeviceState.Ready, "Connection Lost");
        ActivityHelper.INSTANCE.showSnack(binding.coordinator, "Connection Lost");

        //status("connection lost: " + e.getMessage()); //TODO: This message should be in some kind of log
        disconnect();
    }




    private void receive(byte[] data) {

        if (hexEnabled) {
        } else {
            String msg = new String(data);

            if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
            }
            elocReceive(msg);
        }
    }

    private void elocReceive(String msg) {
        // Save device settings; but we can see that tthe value was actually saved. So maybe the firmware need to do a follow
        // let check the old project.
        if (msg.startsWith("#")) {
            preferencesHelper.setDeviceSettings(msg);
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

        long lastGoogleTimestamp = preferencesHelper.getLastGoogleTimestamp();
        data = data.trim() + "\nApp last time sync:  " + Long.toString(((System.currentTimeMillis() - lastGoogleTimestamp) / 1000l / 60l)) + " min\n";
        data = data + "App Version:  " + App.Companion.getVersionName();

        String temp = deviceAddress.replace(":", "-");
        String filename = temp + ".txt";
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {

        }
    }

    private SpannableStringBuilder spanWhite(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.white)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //receiveText.append(spn);
        return (spn);
    }

    private void setActionBarText(String text) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(text);
        }
    }

    private void setDeviceInfo(String msg) {
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

        if (lines.length > 0) { //
            int recON = 0;
            // got an update this time
            for (String l : lines) {
                if (l.startsWith("Ranger:")) {
                    String rangerName = l.replace("Ranger:", "").trim();
                    binding.rangerNameTv.setText(rangerName);
                } else if (l.startsWith("!0!")) {
                    String deviceName = l.replace("!0!", "").trim();
                    setActionBarText(deviceName);
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
                    LabelColor batteryValueColor = LabelColor.Middle;
                    if (tmp.contains("low")) {
                        batteryValueColor = LabelColor.Off;
                    } else if (tmp.contains("full")) {
                        batteryValueColor = LabelColor.On;
                    }
                    setLabelColor(binding.batteryValueTv, batteryValueColor, true);

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
                    String tmp = l.replace("!6!", "").trim();
                    recordingTime = parseDouble(tmp);
                    setRecordingTime();
                } else if (l.startsWith("!7!")) {
                    if (l.contains("1")) {
                        updateDeviceState(DeviceState.Recording, null);
                        recON = 1;
                    } else {
                        updateDeviceState(DeviceState.Ready, null);
                        recON = 0;
                    }
                    setRecordingTime();
                } else if (l.startsWith("!8!")) {
                    // !8! tell the state of bluetooth when recording, so I do not understand while
                    // we are changing/resetting the recording time to zero here
                    // I will just leave this block of code here, in case it fixed some logic issue
                    // I cannot figure out right now.
                    if (recordingTime <= 0) {
                        String newRecordingTime = l.replace("!8!", "").toUpperCase().trim();
                        if (newRecordingTime.equalsIgnoreCase("on")) {
                            // Set to zero if for some weird reason recording time was not yet already received
                            recordingTime = 0;
                            setRecordingTime();
                        }
                    }

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
                    if (usedGB != null) {
                        setLabelColor(
                                binding.sdCardValueTv,
                                usedGB < 40 ? LabelColor.off : LabelColor.on,
                                true
                        );
                    }
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
                        final LabelColor labelColor;
                        if (accuracy < 5) {
                            labelColor = LabelColor.on;
                        } else if (accuracy < 10) {
                            labelColor = LabelColor.middle;
                        } else {
                            labelColor = LabelColor.off;
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
                    preferencesHelper.setDeviceSettings(settings);
                }
                if ((micGain != null) && (micType != null)) {
                    String settings = String.format(
                            Locale.ENGLISH,
                            "#%s#%s",
                            micType, micGain);
                    preferencesHelper.setMicrophoneSettings(settings);
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
        if (rawValue == null) {
            rawValue = "";
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < rawValue.length(); i++) {
            char c = rawValue.charAt(i);
            if (Character.isDigit(c) || (c == '.')) {
                buffer.append(c);
            }
        }
        Double result = null;
        try {
            result = Double.parseDouble(buffer.toString().trim());
        } catch (NumberFormatException ignore) {
        }
        return result;
    }

    private void setTime(TextView textView, String time) {
        String prettyTime = getString(R.string.off);
        if (time != null && (!time.contains("0.00"))) {
            Double tmp = parseDouble(time);
            if (tmp != null) {
                prettyTime = ActivityHelper.INSTANCE.getPrettifiedDuration(tmp);
            }
        }
        textView.setText(prettyTime);
    }

    private void setRecordingTime() {
        String text = getString(R.string.off);
        LabelColor color = LabelColor.off;
        if (deviceState == DeviceState.Recording) {
            Double duration = recordingTime;
            if (deviceState == DeviceState.Stopping) {
                duration = Double.valueOf("0");
            }
            if (duration != null) {
                text = ActivityHelper.INSTANCE.getPrettifiedDuration(duration);
                color = LabelColor.on;
            }
        }
        binding.recordingValueTv.setText(text);
        setLabelColor(binding.recordingValueTv, color, true);
    }

    private String getBestTimeEstimate() {
        //returns a string with the following:
        //X_Y_ZZZZZZZZZZZZZZZ

        long lastGoogleTimestamp = preferencesHelper.getLastGoogleTimestamp();
        long previouselapsedtime = preferencesHelper.getCurrentElapsedTime();
        long currentElapsedTime = SystemClock.elapsedRealtime();
        long elapsedTimeDifferenceMinutes = (currentElapsedTime - previouselapsedtime) / 1000L / 60L;
        long elapsedTimeDifferenceMS = (currentElapsedTime - previouselapsedtime);
        String X, Y, timestamp;

        if ((lastGoogleTimestamp == 0L) || (elapsedTimeDifferenceMinutes > (48 * 60))) {
            X = "P";
            Y = Long.toString(elapsedTimeDifferenceMinutes);
            timestamp = X + "__" + Y + "___" + System.currentTimeMillis();
        } else {
            X = "G";
            Y = Long.toString(elapsedTimeDifferenceMinutes);
            timestamp = X + "__" + Y + "___" + (lastGoogleTimestamp + elapsedTimeDifferenceMS);
        }
        return (timestamp);
    }


  */
    }
} //1070