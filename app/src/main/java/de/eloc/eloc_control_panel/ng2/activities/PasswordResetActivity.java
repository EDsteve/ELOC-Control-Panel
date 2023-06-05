package de.eloc.eloc_control_panel.ng2.activities;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityPasswordResetBinding;

public class PasswordResetActivity extends ThemableActivity {
    private ActivityPasswordResetBinding binding;
    private UserAccountViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordResetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);
        updateUI(false);
        setListeners();
    }

    private void setListeners() {
        binding.backButton.setOnClickListener(v -> onBackPressed());
        binding.sendButton.setOnClickListener(v -> sendResetLink());
        binding.emailAddressTextInput.addTextChangedListener(new TextInputWatcher(binding.emailAddressLayout));
        binding.root.setOnClickListener(v -> ActivityHelper.INSTANCE.hideKeyboard(PasswordResetActivity.this));
    }

    private void updateUI(boolean locked) {
        if (locked) {
            binding.emailAddressLayout.setEnabled(false);
            binding.sendButton.setEnabled(false);
            binding.progressIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.emailAddressLayout.setEnabled(true);
            binding.sendButton.setEnabled(true);
            binding.progressIndicator.setVisibility(View.GONE);
        }
    }

    private void sendResetLink() {
        binding.emailAddressLayout.setError(null);

        Editable editable = binding.emailAddressTextInput.getEditableText();
        String emailAddress = "";
        if (editable != null) {
            emailAddress = editable.toString().trim();
        }
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.setError(getText(R.string.email_address_is_required));
            return;
        }

        updateUI(true);
        viewModel.sendPasswordResetLink(emailAddress, this::onResult);
    }

    private void onResult(boolean sent) {
        updateUI(false);
        if (sent) {
            String title = getString(R.string.link_sent);
            String message = getString(R.string.reset_link_sent_message);
            JavaActivityHelper.showModalAlert(this, title, message, this::onBackPressed);
        } else {
            JavaActivityHelper.showModalAlert(this, getString(R.string.oops), getString(R.string.send_reset_link_error));
        }
    }
}