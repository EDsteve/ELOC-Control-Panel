package de.eloc.eloc_control_panel.helpers;

import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

public class Helper {
    public static void showAlert(Context context, String message) {
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, i) -> dialog.dismiss())
                .show();
    }


    public static void showSnack(CoordinatorLayout coordinator, String message) {
        Snackbar
                .make(coordinator, message, Snackbar.LENGTH_LONG)
                .show();
    }

    public static void hideKeyboard(AppCompatActivity activity) {
        try {
            if (activity != null) {
                InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (manager != null) {
                    View view = activity.getCurrentFocus();
                    if (view != null) {
                        IBinder binder = view.getWindowToken();
                        if (binder != null) {
                            manager.hideSoftInputFromWindow(binder, 0);
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            // It an error occurs trying to hide the keyboard,
            // just ignore the error without crashing app.
        }
    }

}
