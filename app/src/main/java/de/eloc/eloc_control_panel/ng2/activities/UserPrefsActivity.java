package de.eloc.eloc_control_panel.ng2.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CompoundButton;

import de.eloc.eloc_control_panel.databinding.ActivityUserPrefsBinding;
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper;
import de.eloc.eloc_control_panel.ng2.models.PreferredFontSize;

public class UserPrefsActivity extends AppCompatActivity {
    private ActivityUserPrefsBinding binding;
    private PreferredFontSize preferredFontSize = PreferredFontSize.small;
    private PreferencesHelper helper = PreferencesHelper.Companion.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserPrefsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setToolBar();
        loadPrefs();
        setListeners();
    }

    private void setToolBar() {
        setSupportActionBar(binding.appbar.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setListeners() {
        binding.radFontSmall.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                preferredFontSize = PreferredFontSize.small;
                setPreferredFont();
            }
        });
        binding.radFontMedium.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                preferredFontSize = PreferredFontSize.medium;
                setPreferredFont();
            }
        });
        binding.radFontLarge.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                preferredFontSize = PreferredFontSize.large;
                setPreferredFont();
            }
        });
    }

    private void loadPrefs() {

        int fontSize = helper.getPreferredFontSize();
        preferredFontSize = PreferredFontSize.fromInt(fontSize);
        switch (preferredFontSize) {
            case small:
                binding.radFontSmall.setChecked(true);
                break;
            case medium:
                binding.radFontMedium.setChecked(true);
                break;
            case large:
                binding.radFontLarge.setChecked(true);
        }
    }

    private void setPreferredFont() {
        helper.setPreferredFontSize(preferredFontSize.getSize());
    }
}