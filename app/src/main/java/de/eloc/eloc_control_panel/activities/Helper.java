package de.eloc.eloc_control_panel.activities;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

public class Helper {
    public static void showSnack(CoordinatorLayout coordinator, String message) {
        Snackbar
                .make( coordinator, message, Snackbar.LENGTH_LONG)
                .show();
    }
}
