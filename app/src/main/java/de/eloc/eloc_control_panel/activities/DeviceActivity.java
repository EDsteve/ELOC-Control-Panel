package de.eloc.eloc_control_panel.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.MenuItem;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.TerminalFragment;
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding;

public class DeviceActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    ActivityDeviceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setActionBar();

        Bundle extras = getIntent().getExtras();
        boolean hasDevice = false;
        FragmentManager manager = getSupportFragmentManager();
        if ((extras != null) && (manager != null)) {
            hasDevice = extras.containsKey("device");
            Fragment fragment = new TerminalFragment();
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
}