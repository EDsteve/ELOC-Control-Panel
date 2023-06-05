package de.eloc.eloc_control_panel.ng2.activities;

import androidx.appcompat.app.ActionBar;

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
