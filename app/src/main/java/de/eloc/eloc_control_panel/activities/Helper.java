package de.eloc.eloc_control_panel.activities;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

public class Helper {
    public static void showSnack(CoordinatorLayout coordinator, String message) {
        Snackbar
                .make( coordinator, message, Snackbar.LENGTH_LONG)
                .show();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager.isAcceptingText()) {
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(),
                    0
            );
        }
    }
}
