package de.eloc.eloc_control_panel.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.Locale;

import de.eloc.eloc_control_panel.App;
import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.databinding.ActivityMainSettingsBinding;

public class MainSettingsActivity extends AppCompatActivity {
    ActivityMainSettingsBinding binding;

    private enum GainType {
        High(11), // Old forest  (HIGH)
        Low(14); // Old mahout (LOW)

        private int intValue;

        GainType(int i) {
            intValue = i;
        }

        public static GainType fromValue(Integer i) {
            if (i == null) {
                return GainType.High;
            } else if (i == 14) {
                return GainType.Low;
            } else {
                return GainType.High;
            }
        }
    }

    public static String DATA_KEY = "device_settings";
    public static String MIC_DATA_KEY = "microphone_settings";
    private String gPattern = "^[a-zA-Z0-9]+$"; // Pattern for filename
    private String gLocation = "";
    private String gSecondsPerFile = "";
    private String gSamplesPerSec = "";
    public static final String COMMAND = "command";
    private String micType = "";
    private GainType micGain = GainType.High;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setData();
        setMicData();
        setToolbar();
        setListeners();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    private void setToolbar() {
        setSupportActionBar(binding.appbar.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("ELOC Settings");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setMicData() {
        String data = App.getInstance().getSharedPrefs().getString(MIC_DATA_KEY, "");
        String[] separated = data.split("#");
        if (separated.length < 2) {
            return;
        }

        micType = separated[1].trim();
        binding.micTypeEt.setText(micType);

        String gain = separated[2].trim();
        Integer gainObject = null;
        try {
            gainObject = Integer.parseInt(gain);
        } catch (NumberFormatException ignore) {

        }
        micGain = GainType.fromValue(gainObject);
        switch (micGain) {
            case Low:
                binding.radLow.setChecked(true);
                break;
            case High:
                binding.radHigh.setChecked(true);
                break;
        }
    }

    private void setData() {
        String data = App.getInstance().getSharedPrefs().getString(DATA_KEY, "");
        //gInitialSettings=true;
        //
        // #16000 sample rate /0
        // #10 seconds /1
        // #loc /2
        // First item (0) is not used, so firmware dev was counting from 1, not 0.
        String[] separated = data.split("#");
        if (separated.length < 4) {
            return;
        }
        Integer sampleRateObject = null;
        try {
            sampleRateObject = Integer.parseInt(separated[1]);
        } catch (NumberFormatException ignore) {

        }
        int sampleRate = (sampleRateObject == null) ? 16000 : sampleRateObject;
        if (sampleRate <= 8000) {
            binding.rad8k.setChecked(true);
        } else if (sampleRate <= 16000) {
            binding.rad16k.setChecked(true);
        } else if (sampleRate <= 22050) {
            binding.rad22k.setChecked(true);
        } else if (sampleRate <= 32000) {
            binding.rad32k.setChecked(true);
        } else if (sampleRate <= 44100) {
            binding.rad44k.setChecked(true);
        }
        gSamplesPerSec = String.valueOf(sampleRate);

        Integer seconds = null;
        try {
            seconds = Integer.parseInt(separated[2]);
        } catch (NumberFormatException ignore) {

        }
        if (seconds == null) {
            seconds = 3600;
        }
        if (seconds <= 10) {
            binding.rad10s.setChecked(true);
        } else if (seconds <= 60) {
            binding.rad1m.setChecked(true);
        } else if (seconds <= 3600) {
            binding.rad1h.setChecked(true);
        } else if (seconds <= 14400) {
            binding.rad4h.setChecked(true);
        } else if (seconds <= 43200) {
            binding.rad12h.setChecked(true);
        }
        gSecondsPerFile = String.valueOf(seconds);

        gLocation = separated[3].trim();
        binding.elocBtNameEt.setText(gLocation);
    }

    private void setListeners() {
        binding.radioGroupSamplesPerSec.setOnCheckedChangeListener(this::samplesPeSecChanged);
        binding.radioGroupSecPerFile.setOnCheckedChangeListener(this::secPerFileChanged);
        binding.radioGroupGain.setOnCheckedChangeListener(this::gainChanged);
        binding.commandLineBtn.setOnClickListener(view -> runCommandLine());
        binding.recordingBtn.setOnClickListener(view -> runRecordingCommand());
        binding.microphoneTypeBtn.setOnClickListener(view -> runMicTypeCommand());
        binding.microphoneGainBtn.setOnClickListener(view -> runMicGainCommand());
        binding.fileheaderBtn.setOnClickListener(view -> runFileHeaderCommand());
    }

    private void runCommand(String command) {
        Helper.hideKeyboard(this);
        if (!command.startsWith("#settings#")) {
            command = "#settings#" + command; // This should make it easy to enter showrt commands without the prefix.
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra(COMMAND, command);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void runCommandLine() {
        String command = getValue(binding.customCommandEt.getText());
        if (command.isEmpty()) {
            Helper.showSnack(binding.coordinator, "You must enter a command to run!");
            return;
        }
        runCommand(command);
    }

    private void runFileHeaderCommand() {
        String command = getValue(binding.fileheaderEt.getText());
        if (command.isEmpty()) {
            Helper.showSnack(binding.coordinator, "You must enter a Device Name!");
            return;
        }
        String suffix = "setname";
        if (!command.endsWith(suffix)) {
            command = command + suffix;
        }
        runCommand(command);
    }

    private void runMicTypeCommand() {
        String type = getValue(binding.micTypeEt.getText());
        if (type.isEmpty()) {
            Helper.showSnack(binding.coordinator, "You must enter a mic type to set!");
            return;
        }
        String command = String.format(Locale.ENGLISH, "%ssettype", type);
        runCommand(command);
    }

    private void runMicGainCommand() {
        int gain = micGain.intValue;
        String command = String.format(Locale.ENGLISH, "%dsetgain", gain);
        runCommand(command);
    }

    private void runRecordingCommand() {
        Editable editable = binding.elocBtNameEt.getText();
        if (editable != null) {
            gLocation = editable.toString().trim();
        }
        if (gLocation.isEmpty()) {
            Helper.showSnack(binding.coordinator, "File header name is required!");
            return;
        } else if (!gLocation.matches(gPattern)) {
            Helper.showSnack(binding.coordinator, "Invalid file header name!");
            return;
        }

        String command = "#settings" + "#" + gSamplesPerSec + "#" + gSecondsPerFile + "#" + gLocation;
        runCommand(command);
    }

    private String getValue(Editable editable) {
        String val = "";
        if (editable != null) {
            val = editable.toString().trim();
        }
        return val;
    }

    public void samplesPeSecChanged(RadioGroup group, int checkedId) {
        // checkedId is the RadioButton selected
        //if (gInitialSettings) return;
        // if (!group.isPressed())
        // {

        // return;
        // }
        Log.i("elocApp", "samplerate buttonpress");

        switch (checkedId) {
            case R.id.rad8k:
                gSamplesPerSec = "8000";
                break;
            case R.id.rad16k:
                gSamplesPerSec = "16000";
                break;
            case R.id.rad22k:
                gSamplesPerSec = "22050";
                break;
            case R.id.rad32k:
                gSamplesPerSec = "32000";
                break;

            case R.id.rad44k:
                gSamplesPerSec = "44100";
                break;
        }

        //sendText.setText(String.valueOf(checkedId));
    }

    public void secPerFileChanged(RadioGroup group, int checkedId) {

        Log.i("elocApp", "secperfile buttonpress");
        switch (checkedId) {
            case R.id.rad10s:
                gSecondsPerFile = "10";
                break;
            case R.id.rad1m:
                gSecondsPerFile = "60";
                break;
            case R.id.rad1h:
                gSecondsPerFile = "3600";
                break;
            case R.id.rad4h:
                gSecondsPerFile = "14400";
                break;
            case R.id.rad12h:
                gSecondsPerFile = "43200";
                break;
        }
    }

    public void gainChanged(RadioGroup group, int checkedId) {

        Log.i("elocApp", "micgain buttonpress");
        switch (checkedId) {
            case R.id.radHigh:
                micGain = GainType.High;
                break;
            case R.id.radLow:
                micGain = GainType.Low;
                break;
        }
    }
}
