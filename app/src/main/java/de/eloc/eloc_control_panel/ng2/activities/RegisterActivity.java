package de.eloc.eloc_control_panel.ng2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityRegisterBinding;

public class RegisterActivity extends NoActionBarActivity {
    private ActivityRegisterBinding binding;
    private UserAccountViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);

        setListeners();
        setWatchers();
        updateUI(false);
    }

    private void setWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(new TextInputWatcher(binding.emailAddressLayout));
        binding.passwordTextInput.addTextChangedListener(new TextInputWatcher(binding.passwordLayout));
        binding.verifyPasswordTextInput.addTextChangedListener(new TextInputWatcher(binding.verifyPasswordLayout));
    }

    private void setListeners() {
        binding.backButton.setOnClickListener(v -> onBackPressed());
        binding.loginButton.setOnClickListener(v -> onBackPressed());
        binding.registerButton.setOnClickListener(v -> register());
        binding.root.setOnClickListener(v -> ActivityHelper.INSTANCE.hideKeyboard(RegisterActivity.this));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void register() {
        ActivityHelper.INSTANCE.hideKeyboard(this);
        binding.emailAddressLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.verifyPasswordLayout.setError(null);
        String emailAddress = "";
        Editable editable = binding.emailAddressTextInput.getEditableText();
        if (editable != null) {
            emailAddress = editable.toString().trim();
        }
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.setError(getString(R.string.email_address_is_required));
            binding.emailAddressTextInput.requestFocus();
            return;
        }

        final int passwordMinLength = getResources().getInteger(R.integer.password_min_length);
        String password = "";
        editable = binding.passwordTextInput.getEditableText();
        if (editable != null) {
            password = editable.toString();
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError(getString(R.string.password_is_required));
            binding.passwordTextInput.requestFocus();
            return;
        } else if (password.length() < passwordMinLength) {
            binding.passwordLayout.setError(getString(R.string.password_too_short));
            binding.passwordTextInput.requestFocus();
            return;
        }

        String verify = "";
        editable = binding.verifyPasswordTextInput.getEditableText();
        if (editable != null) {
            verify = editable.toString();
        }
        if (!verify.equals(password)) {
            binding.verifyPasswordLayout.setError(getString(R.string.passwords_must_match));
            binding.verifyPasswordTextInput.requestFocus();
            return;
        }

        updateUI(true);
        viewModel.register(emailAddress, password, this::registrationHandler);
    }

    private void registrationHandler(String error) {
        updateUI(false);
        if (error == null) {
            error = "";
        }
        error = error.trim();
        if (error.isEmpty()) {
            JavaActivityHelper.open(this, VerifyEmailActivity.class, true);
        } else {
            JavaActivityHelper.showModalAlert(this, getString(R.string.oops), error);
        }
    }

    private void updateUI(boolean locked) {
        binding.progressHorizontal.setVisibility(locked ? View.VISIBLE : View.INVISIBLE);
        binding.emailAddressLayout.setEnabled(!locked);
        binding.passwordLayout.setEnabled(!locked);
        binding.verifyPasswordLayout.setEnabled(!locked);
        binding.registerButton.setEnabled(!locked);
    }
}