package de.eloc.eloc_control_panel.ng2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import de.eloc.eloc_control_panel.R;
import de.eloc.eloc_control_panel.data.UserAccountViewModel;
import de.eloc.eloc_control_panel.databinding.ActivityLoginBinding;

public class LoginActivity extends NoActionBarActivity {
    private ActivityLoginBinding binding;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private UserAccountViewModel viewModel;
    private boolean googleSignInActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(UserAccountViewModel.class);
        setListeners();
        setLaunchers();
        setTextWatchers();
        updateUIForGoogleSignIn();
        updateUI(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthState(false);
    }

    private void setTextWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(new TextInputWatcher(binding.emailAddressLayout));
        binding.passwordTextInput.addTextChangedListener(new TextInputWatcher(binding.passwordLayout));
    }

    private void setLaunchers() {
        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            viewModel.signInWithGoogle(data, this::checkAuthState);
        });
    }

    private void setListeners() {
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        binding.registerButton.setOnClickListener(v -> openRegisterActivity());
        binding.loginButton.setOnClickListener(v -> login());
        binding.root.setOnClickListener(v -> ActivityHelper.INSTANCE.hideKeyboard(LoginActivity.this));
        binding.resetPasswordButton.setOnClickListener(v -> JavaActivityHelper.open(this, PasswordResetActivity.class, false));
    }

    private void openRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }

    private void signInWithGoogle() {
        googleSignInActive = true;
        updateUIForGoogleSignIn();
        Intent intent = viewModel.getGoogleSignInIntent();
        googleSignInLauncher.launch(intent);
    }

    private void checkAuthState(boolean completingSignIn) {
        if (completingSignIn) {
            googleSignInActive = false;
        }
        if (googleSignInActive) {
            return;
        }

        if (viewModel.isSignedIn()) {
            if (viewModel.isEmailVerified()) {
                viewModel.hasProfile(profileFound -> {
                    if (profileFound) {
                        JavaActivityHelper.open(LoginActivity.this, LandingActivity.class, true);
                    } else {
                        JavaActivityHelper.open(LoginActivity.this, SetupActivity.class, true);
                    }
                });
            } else {
                JavaActivityHelper.open(this, VerifyEmailActivity.class, true);
            }
        }
        updateUIForGoogleSignIn();
    }

    private void updateUIForGoogleSignIn() {
        binding.loginProgressIndicator.setVisibility(View.GONE);
        if (googleSignInActive) {
            binding.googleSignInButton.setEnabled(false);
            binding.googleSignInProgressIndicator.setEnabled(true);
            binding.googleSignInProgressIndicator.setVisibility(View.VISIBLE);
            binding.emailAddressLayout.setEnabled(false);
            binding.passwordLayout.setEnabled(false);
            binding.resetPasswordButton.setEnabled(false);
            binding.loginButton.setEnabled(false);
            binding.registerButton.setEnabled(false);
        } else {
            binding.googleSignInButton.setEnabled(true);
            binding.googleSignInProgressIndicator.setEnabled(false);
            binding.googleSignInProgressIndicator.setVisibility(View.GONE);
            binding.emailAddressLayout.setEnabled(true);
            binding.passwordLayout.setEnabled(true);
            binding.resetPasswordButton.setEnabled(true);
            binding.loginButton.setEnabled(true);
            binding.registerButton.setEnabled(true);
        }
    }

    private void updateUI(boolean locked) {
        binding.googleSignInProgressIndicator.setVisibility(View.GONE);
        if (locked) {
            binding.googleSignInButton.setEnabled(false);
            binding.loginProgressIndicator.setEnabled(true);
            binding.loginProgressIndicator.setVisibility(View.VISIBLE);
            binding.emailAddressLayout.setEnabled(false);
            binding.passwordLayout.setEnabled(false);
            binding.resetPasswordButton.setEnabled(false);
            binding.loginButton.setEnabled(false);
            binding.registerButton.setEnabled(false);
        } else {
            binding.googleSignInButton.setEnabled(true);
            binding.loginProgressIndicator.setEnabled(false);
            binding.loginProgressIndicator.setVisibility(View.GONE);
            binding.emailAddressLayout.setEnabled(true);
            binding.passwordLayout.setEnabled(true);
            binding.resetPasswordButton.setEnabled(true);
            binding.loginButton.setEnabled(true);
            binding.registerButton.setEnabled(true);
        }
    }

    private void login() {
        binding.emailAddressLayout.setError(null);
        binding.passwordLayout.setError(null);

        Editable editable = binding.emailAddressTextInput.getEditableText();
        String emailAddress = "";
        if (editable != null) {
            emailAddress = editable.toString().trim();
        }
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.setError(getText(R.string.email_address_is_required));
            return;
        }

        String password = "";
        editable = binding.passwordTextInput.getEditableText();
        if (editable != null) {
            password = editable.toString();
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError(getText(R.string.password_is_required));
            return;
        }

        updateUI(true);
        viewModel.signIn(emailAddress, password, this::onLoginCompleted);
    }

    private void onLoginCompleted(String error) {
        updateUI(false);
        if (error == null) {
            error = "";
        }
        error = error.trim();
        if (error.isEmpty()) {
            checkAuthState(false);
        } else {
            JavaActivityHelper.showModalAlert(this, getString(R.string.oops), error);
        }
    }
}