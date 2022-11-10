package de.eloc.eloc_control_panel;

import android.app.Application;

import android.content.SharedPreferences;

import de.eloc.eloc_control_panel.helpers.BluetoothHelper;

public class App extends Application {
    private static App mAppInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        BluetoothHelper.initialize();
    }

    public static App getInstance() {
        return mAppInstance;
    }

    public SharedPreferences getSharedPrefs() {
        // TODO: I will upgrade it to the support library for compatibility with more devices.
        // I do not remember the package name for the support library at the moment
        // but app will still work :)
        return getSharedPreferences("label", 0);
    }
}
