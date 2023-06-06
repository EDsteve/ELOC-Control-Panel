package de.eloc.eloc_control_panel.ng2.activities;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityVerifyEmailBinding;
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback;

public class VerifyEmailActivity extends NoActionBarActivity {
    private ActivityVerifyEmailBinding binding;
    private UserAccountViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthState();
    }

    private void setListeners() {
        binding.verifiedButton.setOnClickListener(v -> signIn());
        binding.resendButton.setOnClickListener(v -> viewModel.sendEmailVerificationLink(this::onResendCompleted));
        binding.signOutButton.setOnClickListener(v -> {
            viewModel.signOut();
            JavaActivityHelper.open(this, LoginActivity.class, true);
        });
    }

    private void updateUI(boolean showProgress) {
        if (showProgress) {
            binding.progressHorizontal.setVisibility(View.VISIBLE);
            binding.messageLayout.setVisibility(View.GONE);
        } else {
            binding.progressHorizontal.setVisibility(View.GONE);
            binding.messageLayout.setVisibility(View.VISIBLE);
        }
    }

    private void onHasProfileCompleted(boolean hasProfile) {
        if (hasProfile) {
            JavaActivityHelper.open(this, HomeActivity.class, true);
        } else {
            JavaActivityHelper.open(this, ProfileSetupActivity.class, true);
        }
    }

    private void onResendCompleted(boolean sent) {
        String title, message;
        if (sent) {
            title = getString(R.string.link_sent);
            message = getString(R.string.resend_message);
        } else {
            title = getString(R.string.oops);
            message = getString(R.string.resend_failed);
        }
        JavaActivityHelper.showModalAlert(this, title, message);
    }

    private void signIn() {
        String title = getString(R.string.email_verification);
        String message = getString(R.string.redirect_to_sign_in);
        VoidCallback callback = () -> {
            viewModel.signOut();
            JavaActivityHelper.open(VerifyEmailActivity.this, LoginActivity.class, true);
        };
        JavaActivityHelper.showModalAlert(this, title, message, callback);
    }

    private void checkAuthState() {
        updateUI(true);
        if (viewModel.isSignedIn()) {
            if (viewModel.isEmailVerified()) {
                viewModel.hasProfile(this::onHasProfileCompleted);
            } else {
                String message = getString(R.string.verification_message, viewModel.getEmailAddress());
                binding.messageTextView.setText(message);
                updateUI(false);
            }
        } else {
            JavaActivityHelper.open(this, LoginActivity.class, true);
        }
    }
}