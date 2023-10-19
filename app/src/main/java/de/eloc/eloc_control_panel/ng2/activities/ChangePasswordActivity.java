package de.eloc.eloc_control_panel.ng2.activities;

import static de.eloc.eloc_control_panel.ng3.activities.ActivityExtensionsKt.hideKeyboard;
import static de.eloc.eloc_control_panel.ng3.activities.ActivityExtensionsKt.showModalAlert;

import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityChangePasswordBinding;
import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity;

public class ChangePasswordActivity extends ThemableActivity {

    private ActivityChangePasswordBinding binding;
    private UserAccountViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);

        setToolbar();
        setListeners();
        updateUI(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    private void updateUI(boolean locked) {
        if (locked) {
            binding.currentPasswordLayout.setEnabled(false);
            binding.newPasswordLayout.setEnabled(false);
            binding.verifyPasswordLayout.setEnabled(false);
            binding.submitButton.setEnabled(false);
            binding.progressHorizontal.setVisibility(View.VISIBLE);
        } else {
            binding.currentPasswordLayout.setEnabled(true);
            binding.newPasswordLayout.setEnabled(true);
            binding.verifyPasswordLayout.setEnabled(true);
            binding.submitButton.setEnabled(true);
            binding.progressHorizontal.setVisibility(View.GONE);
        }
    }

    private void setToolbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.change_password);
        }
    }

    private void setListeners() {
        binding.root.setOnClickListener(v -> hideKeyboard(this));
        binding.submitButton.setOnClickListener(v -> submit());

        binding.currentPasswordTextInput.addTextChangedListener(new TextInputWatcher(binding.currentPasswordLayout));
        binding.newPasswordTextInput.addTextChangedListener(new TextInputWatcher(binding.newPasswordLayout));
        binding.verifyPasswordTextInput.addTextChangedListener(new TextInputWatcher(binding.verifyPasswordLayout));
        binding.verifyPasswordTextInput.addTextChangedListener(new TextInputWatcher(binding.newPasswordLayout));
    }

    private void submit() {
        hideKeyboard(this);
        binding.currentPasswordLayout.setError(null);
        binding.newPasswordLayout.setError(null);
        binding.verifyPasswordLayout.setError(null);

        String currentPassword = "";
        Editable editable = binding.currentPasswordTextInput.getEditableText();
        if (editable != null) {
            currentPassword = editable.toString();
        }
        if (currentPassword.isEmpty()) {
            binding.currentPasswordLayout.setError(getString(R.string.required));
            return;
        }

        final int passwordMinLength = getResources().getInteger(R.integer.password_min_length);
        String newPassword = "";
        editable = binding.newPasswordTextInput.getEditableText();
        if (editable != null) {
            newPassword = editable.toString();
        }
        if (newPassword.isEmpty()) {
            binding.newPasswordLayout.setError(getString(R.string.required));
            return;
        } else if (newPassword.length() < passwordMinLength) {
            binding.newPasswordLayout.setError(getString(R.string.password_too_short));
            return;
        }

        String verifyPassword = "";
        editable = binding.verifyPasswordTextInput.getEditableText();
        if (editable != null) {
            verifyPassword = editable.toString();
        }
        if (!newPassword.equals(verifyPassword)) {
            binding.newPasswordLayout.setError(getString(R.string.passwords_must_match));
            return;
        }

        updateUI(true);
        viewModel.changePassword(newPassword, currentPassword, this::newPasswordSubmitted);
    }

    private void newPasswordSubmitted(String error) {
        if (error == null) {
            error = "";
        }
        error = error.trim();
        String title, message;
        boolean goBack = false;
        if (error.isEmpty()) {
            title = getString(R.string.account);
            message = getString(R.string.password_updated);
            goBack = true;
        } else {
            title = getString(R.string.oops);
            message = error;
        }
        if (goBack) {
            showModalAlert(this, title, message, this::onBackPressed);
        } else {
            showModalAlert(this, title, message);
            updateUI(false);
        }
    }
}