package de.eloc.eloc_control_panel.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.TerminalFragment;
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding;

public class DeviceActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    ActivityDeviceBinding binding;

    public ActivityResultLauncher<Intent> settingsLauncher;
    private TerminalFragment fragment = new TerminalFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setActionBar();
        setLaunchers();

        Bundle extras = getIntent().getExtras();
        boolean hasDevice = false;
        FragmentManager manager = getSupportFragmentManager();
        if ((extras != null) && (manager != null)) {
            hasDevice = extras.containsKey("device");
            fragment.setArguments(extras);
            manager.beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
        }

        if (!hasDevice) {
            finish();
        }
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

    @Override
    public void onBackStackChanged() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            boolean homeAsUpEnabled = (getSupportFragmentManager().getBackStackEntryCount() > 0);
            actionBar.setDisplayHomeAsUpEnabled(homeAsUpEnabled);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
                               String resultMessage = fragment.send(command);
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
}