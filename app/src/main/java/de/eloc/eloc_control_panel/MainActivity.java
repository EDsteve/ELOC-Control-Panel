package de.eloc.eloc_control_panel;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.widget.Toast;
import android.os.SystemClock;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
//public static long testme=0L;

/* 	Context context = this;
	boolean requireFineGranularity = false;
	boolean passiveMode = false;
	long updateIntervalInMilliseconds = 10 * 60 * 1000;
	boolean requireNewLocation = false;
	new SimpleLocation(context, requireFineGranularity, passiveMode, updateIntervalInMilliseconds, requireNewLocation);
	 */

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        Log.i("elocApp", "\n\n\n mainActivity onCreate");

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
//            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new TerminalFragment(), "devices").commit();
        } else {
            onBackStackChanged();
        }
    }

    @Override
    public void onBackStackChanged() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            boolean homeAsUpEnabled = (getSupportFragmentManager().getBackStackEntryCount() > 0);
            actionBar.setDisplayHomeAsUpEnabled(homeAsUpEnabled);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
