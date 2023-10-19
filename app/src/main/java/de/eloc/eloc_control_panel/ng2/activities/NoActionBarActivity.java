package de.eloc.eloc_control_panel.ng2.activities;

import androidx.appcompat.app.ActionBar;

import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity;

public class NoActionBarActivity extends ThemableActivity {
    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
}
