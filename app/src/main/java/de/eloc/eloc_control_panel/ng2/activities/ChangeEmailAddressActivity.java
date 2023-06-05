package de.eloc.eloc_control_panel.ng2.activities;

import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityChangeEmailAddressBinding;

public class ChangeEmailAddressActivity extends ThemableActivity {

    private ActivityChangeEmailAddressBinding binding;
    private UserAccountViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangeEmailAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);
        binding.currentEmailAddressTextView.setText(viewModel.getEmailAddress());

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
            binding.textEditLayout.setEnabled(false);
            binding.passwordLayout.setEnabled(false);
            binding.submitButton.setEnabled(false);
            binding.progressHorizontal.setVisibility(View.VISIBLE);
        } else {
            binding.textEditLayout.setEnabled(true);
            binding.passwordLayout.setEnabled(true);
            binding.submitButton.setEnabled(true);
            binding.progressHorizontal.setVisibility(View.GONE);
        }
    }

    private void setToolbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.change_email_address);
        }
    }

    private void setListeners() {
        binding.root.setOnClickListener(v -> ActivityHelper.INSTANCE.hideKeyboard(this));
        binding.submitButton.setOnClickListener(v -> submit());
        binding.editText.addTextChangedListener(new TextInputWatcher(binding.textEditLayout));
        binding.passwordTextInput.addTextChangedListener(new TextInputWatcher(binding.passwordLayout));
    }

    private void submit() {
        ActivityHelper.INSTANCE.hideKeyboard(this);
        binding.textEditLayout.setError(null);
        binding.passwordLayout.setError(null);

        String emailAddress = "";
        Editable editable = binding.editText.getEditableText();
        if (editable != null) {
            emailAddress = editable.toString().trim();
        }
        if (emailAddress.isEmpty()) {
            binding.textEditLayout.setError(getString(R.string.required));
            return;
        }

        String password = "";
        editable = binding.passwordTextInput.getEditableText();
        if (editable != null) {
            password = editable.toString();
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError(getString(R.string.required));
            return;
        }

        updateUI(true);
        viewModel.changeEmailAddress(emailAddress, password, this::emailAddressSubmitted);
    }

    private void emailAddressSubmitted(String error) {
        if (error == null) {
            error = "";
        }
        error = error.trim();
        String title, message;
        boolean goBack = false;
        if (error.isEmpty()) {
            title = getString(R.string.account);
            message = getString(R.string.email_address_updated);
            goBack = true;
        } else {
            title = getString(R.string.oops);
            message = error;
        }
        if (goBack) {
            JavaActivityHelper.showModalAlert(
                    this,
                    title,
                    message,
                    () -> JavaActivityHelper.open(
                            ChangeEmailAddressActivity.this,
                            VerifyEmailActivity.class,
                            true
                    )
            );
        } else {
            JavaActivityHelper.showModalAlert(this, title, message);
            updateUI(false);
        }
    }
}