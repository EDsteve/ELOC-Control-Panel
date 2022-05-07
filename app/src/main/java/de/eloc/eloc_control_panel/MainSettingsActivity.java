package de.eloc.eloc_control_panel;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import de.eloc.eloc_control_panel.databinding.ActivityMainSettingsBinding;

public class MainSettingsActivity extends AppCompatActivity {
    ActivityMainSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO: Set defaults for radio buttons
    }
}