package de.eloc.eloc_control_panel.ng2.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityLandingBinding;
import de.eloc.eloc_control_panel.databinding.LayoutNavHeaderBinding;
import de.eloc.eloc_control_panel.ng2.models.HttpHelper;

public class LandingActivity extends ThemableActivity {
    private ActivityLandingBinding binding;
    private LayoutNavHeaderBinding navHeaderBinding;
    private UserAccountViewModel viewModel;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBindings();
        setContentView(binding.getRoot());
        setViewModel();
        setListeners();
        setToolbar();
        closeDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.getProfile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            toggleDrawer();
            return true;
        } else {
            return onMenuItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.drawer.isOpen()) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    private void setToolbar() {
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("show HomeActivity.java");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.menu);
            actionBar.setHomeActionContentDescription(R.string.open_drawer_menu);
        }
    }

    private void setViewModel() {
        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);
        viewModel.watchProfile().observe(this, userProfile -> {
            navHeaderBinding.profilePictureImageView.setImageUrl(
                    userProfile.getProfilePictureUrl(),
                    HttpHelper.getInstance().getImageLoader()
            );
            navHeaderBinding.userIdTextView.setText(userProfile.getUserId());
            navHeaderBinding.emailAddressTextView.setText(userProfile.getEmailAddress());
        });
    }

    private void setListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::refresh);
        binding.navView.setNavigationItemSelectedListener(this::onNavItemSelected);
        navHeaderBinding.editButton.setOnClickListener(v -> editProfile());
        binding.drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                restoreMenuIcon();
            }
        });
        binding.refreshButton.setOnClickListener(v -> {
            if (!binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(true);
                refresh();
            }
        });
    }

    private void editProfile() {
        closeDrawer();
        JavaActivityHelper.open(this, ProfileActivity.class, false);
    }

    private void editAccount() {
        closeDrawer();
        JavaActivityHelper.open(this, AccountActivity.class, false);
    }

    private boolean onNavItemSelected(MenuItem item) {
        closeDrawer();
        return onMenuItemSelected(item);
    }

    private boolean onMenuItemSelected(MenuItem item) {

        final int id = item.getItemId();
        if (id == R.id.mnu_edit_profile) {
            editProfile();
            return true;
        } else if (id == R.id.mnu_sign_out) {
            signOut();
            return true;
        } else if (id == R.id.mnu_preferences) {
            openAppPreferences();
            return true;
        } else if (id == R.id.mnu_bluetooth_settings) {
            openBluetoothPreferences();
            return true;
        } else if (id == R.id.mnu_find_my_eloc) {
            findMyEloc();
            return true;
        } else if (id == R.id.mnu_browse_eloc_status) {
            browseElocStatus();
            return true;
        } else if (id == R.id.mnu_upload_eloc_status) {
            uploadElocStatus();
            return true;
        } else if (id == R.id.mnu_sync_clock) {
            synchronizeClock();
            return true;
        } else if (id == R.id.mnu_account) {
            editAccount();
            return true;
        } else if (id == R.id.mnu_about) {
            showAboutApp();
            return true;
        } else {
            return false;
        }
    }

    private void showAboutApp() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            String title = getString(R.string.app_name);
            String message = getString(R.string.version_template, info.versionName);
            JavaActivityHelper.showModalAlert(this, title, message);
        } catch (PackageManager.NameNotFoundException ignore) {

        }
    }

    private void setBindings() {
        binding = ActivityLandingBinding.inflate(getLayoutInflater());
        View header = binding.navView.getHeaderView(0);
        navHeaderBinding = LayoutNavHeaderBinding.bind(header);
    }

    private void onSignOut() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void signOut() {
        viewModel.signOut();
        onSignOut();
    }

    private void toggleDrawer() {
        if (binding.drawer.isOpen()) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    private void openDrawer() {
        if (!binding.drawer.isOpen()) {
            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(0); // Show the 'Back/up' arrow
                actionBar.setHomeActionContentDescription(R.string.close_drawer_menu);
            }
            binding.drawer.open();
        }
    }

    private void closeDrawer() {
        if (binding.drawer.isOpen()) {
            restoreMenuIcon();
            binding.drawer.close();
        }
    }

    private void restoreMenuIcon() {
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.menu);
            actionBar.setHomeActionContentDescription(R.string.open_drawer_menu);
        }
    }

    private void openAppPreferences() {
        // todo:
    }

    private void openBluetoothPreferences() {
        // todo:
    }

    private void findMyEloc() {
        // todo:
    }

    private void browseElocStatus() {
        // todo:
    }

    private void uploadElocStatus() {
        // todo:
    }

    private void synchronizeClock() {
        // todo:
    }

    private void refresh() {
        closeDrawer();
        binding.refreshButton.setText(R.string.stop_label);

        // todo:

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.refreshButton.setText(R.string.refresh);
                    Snackbar.make(binding.getRoot(), "Update complete", Snackbar.LENGTH_LONG).show();
                },
                3000
        );
    }
}