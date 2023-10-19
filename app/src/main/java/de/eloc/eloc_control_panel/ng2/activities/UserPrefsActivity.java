package de.eloc.eloc_control_panel.ng2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.databinding.ActivityUserPrefsBinding;
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper;
import de.eloc.eloc_control_panel.ng2.models.PreferredFontSize;
import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity;

public class UserPrefsActivity extends ThemableActivity {
    public static final String EXTRA_FONT_SIZE_CHANGED = "extra_font_size_changed";

    private static final String TAG_LEFT_MENU_CHIP = "left_menu_chip";
    private static final String TAG_RIGHT_MENU_CHIP = "right_menu_chip";

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
        setChips();
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

    private void setChips() {
        binding.leftChipLayout.chip.setTag(TAG_LEFT_MENU_CHIP);
        binding.leftChipLayout.chip.setText(R.string.top_left);
        binding.leftChipLayout.chip.setChecked(false);
        binding.leftChipLayout.chip.setOnCheckedChangeListener(this::menuPositionChanged);
        ActivityHelper.INSTANCE.setChipColors(this, binding.leftChipLayout);

        binding.rightChipLayout.chip.setTag(TAG_RIGHT_MENU_CHIP);
        binding.rightChipLayout.chip.setText(R.string.top_right);
        binding.rightChipLayout.chip.setChecked(false);
        binding.rightChipLayout.chip.setOnCheckedChangeListener(this::menuPositionChanged);
        ActivityHelper.INSTANCE.setChipColors(this, binding.rightChipLayout);

        binding.smallFontChipLayout.chip.setText(R.string.small);
        binding.smallFontChipLayout.chip.setChecked(false);
        ActivityHelper.INSTANCE.setChipColors(this, binding.smallFontChipLayout);

        binding.mediumFontChipLayout.chip.setText(R.string.medium);
        binding.mediumFontChipLayout.chip.setChecked(false);
        ActivityHelper.INSTANCE.setChipColors(this, binding.mediumFontChipLayout);

        binding.largeFontChipLayout.chip.setText(R.string.large);
        binding.largeFontChipLayout.chip.setChecked(false);
        ActivityHelper.INSTANCE.setChipColors(this, binding.largeFontChipLayout);
    }

    private void setListeners() {
        binding.btDevicesSwitch.setOnCheckedChangeListener((compoundButton, checked) -> helper.setShowAllBluetoothDevices(checked));

        binding.smallFontChipLayout.chip.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setPreferredFont(PreferredFontSize.small);
            }
        });
        binding.mediumFontChipLayout.chip.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setPreferredFont(PreferredFontSize.medium);
            }
        });
        binding.largeFontChipLayout.chip.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setPreferredFont(PreferredFontSize.large);
            }
        });
    }

    private void menuPositionChanged(CompoundButton chip, boolean checked) {
        String tag = chip.getTag().toString();
        boolean menuOnLeftSide;
        if (TAG_LEFT_MENU_CHIP.equals(tag)) {
            menuOnLeftSide = checked;
        } else {
            menuOnLeftSide = !checked;
        }
        if (checked) {
            helper.setMainMenuPosition(menuOnLeftSide);
        }
        ActivityHelper.INSTANCE.setChipColors(this, binding.rightChipLayout);
        ActivityHelper.INSTANCE.setChipColors(this, binding.leftChipLayout);
    }

    private void loadPrefs() {
        binding.btDevicesSwitch.setChecked(helper.showingAllBluetoothDevices());
        if (helper.isMainMenuOnLeft()) {
            binding.leftChipLayout.chip.setChecked(true);
        } else {
            binding.rightChipLayout.chip.setChecked(true);
        }
        int fontSize = helper.getPreferredFontSizeValue();
        oldPreferredFontSize = preferredFontSize = PreferredFontSize.fromInt(fontSize);
        switch (preferredFontSize) {
            case small:
                binding.smallFontChipLayout.chip.setChecked(true);
                break;
            case medium:
                binding.mediumFontChipLayout.chip.setChecked(true);
                break;
            case large:
                binding.largeFontChipLayout.chip.setChecked(true);
        }
    }

    private void setPreferredFont(PreferredFontSize newFontSize) {
        preferredFontSize = newFontSize;
        helper.setPreferredFontSize(preferredFontSize.getSize());
        ActivityHelper.INSTANCE.setChipColors(this, binding.smallFontChipLayout);
        ActivityHelper.INSTANCE.setChipColors(this, binding.mediumFontChipLayout);
        ActivityHelper.INSTANCE.setChipColors(this, binding.largeFontChipLayout);
    }
}