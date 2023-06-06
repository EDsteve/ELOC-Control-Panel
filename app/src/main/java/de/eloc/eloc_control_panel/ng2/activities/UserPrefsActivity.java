package de.eloc.eloc_control_panel.ng2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.databinding.ActivityUserPrefsBinding;
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper;
import de.eloc.eloc_control_panel.ng2.models.PreferredFontSize;

public class UserPrefsActivity extends ThemableActivity {
    public static final String EXTRA_FONT_SIZE_CHANGED = "extra_font_size_changed";

    private ActivityUserPrefsBinding binding;
    private PreferredFontSize preferredFontSize = PreferredFontSize.small;
    private PreferredFontSize oldPreferredFontSize = PreferredFontSize.small;
    private final PreferencesHelper helper = PreferencesHelper.Companion.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserPrefsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setToolBar();
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrefs();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (oldPreferredFontSize != preferredFontSize) {
            Intent data = new Intent();
            data.putExtra(EXTRA_FONT_SIZE_CHANGED, true);
            setResult(RESULT_OK, data);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.onBackPressed();
    }

    private void setToolBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setListeners() {
        binding.btDevicesSwitch.setOnCheckedChangeListener((compoundButton, checked) -> helper.setShowAllBluetoothDevices(checked));
        binding.radFontSmall.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setPreferredFont(PreferredFontSize.small);
            }
        });
        binding.radFontMedium.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setPreferredFont(PreferredFontSize.medium);
            }
        });
        binding.radFontLarge.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setPreferredFont(PreferredFontSize.large);
            }
        });
        binding.menuPositionChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean leftSide = (checkedIds.get(0) == R.id.left_chip);
            helper.setMainMenuPosition(leftSide);
            if (leftSide) {
                binding.leftChip.setChipBackgroundColorResource(R.color.colorPrimary);
                binding.rightChip.setChipBackgroundColorResource(R.color.colorPrimaryTranslucent);
            } else {
                binding.leftChip.setChipBackgroundColorResource(R.color.colorPrimaryTranslucent);
                binding.rightChip.setChipBackgroundColorResource(R.color.colorPrimary);
            }
        });
    }

    private void loadPrefs() {
        binding.btDevicesSwitch.setChecked(helper.showingAllBluetoothDevices());
        if (helper.isMainMenuOnLeft()) {
            binding.leftChip.setChecked(true);
        } else {
            binding.rightChip.setChecked(true);
        }
        int fontSize = helper.getPreferredFontSize();
        oldPreferredFontSize = preferredFontSize = PreferredFontSize.fromInt(fontSize);
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

    private void setPreferredFont(PreferredFontSize newFontSize) {
        preferredFontSize = newFontSize;
        helper.setPreferredFontSize(preferredFontSize.getSize());
    }
}