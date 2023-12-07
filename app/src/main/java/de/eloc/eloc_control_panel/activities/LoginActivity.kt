package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityLoginBinding
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import de.eloc.eloc_control_panel.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher
import kotlinx.coroutines.launch

class LoginActivity : ThemableActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: UserAccountViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AuthHelper.instance.registerGoogleSignInListener(::updateUiForGoogleSignIn)
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        setListeners()
        setTextWatchers()
        updateUiForGoogleSignIn()
        updateUI(false)
    }

    override fun onResume() {
        super.onResume()
        val autoGoogleSignIn = PreferencesHelper.instance.getAutoGoogleSignIn()
        val googleSignInCanceled = AuthHelper.instance.googleSignInCanceled
        if (autoGoogleSignIn && (!googleSignInCanceled)) {
            signInWithGoogle()
        } else {
            checkAuthState()
        }
    }

    private fun setTextWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
    }

    private fun setListeners() {
        binding.googleSignInButton.setOnClickListener {
            AuthHelper.instance.clearGoogleSignInCanceled()
            signInWithGoogle()
        }
        binding.registerButton.setOnClickListener { open(RegisterActivity::class.java, true) }
        binding.loginButton.setOnClickListener { login() }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.resetPasswordButton.setOnClickListener {
            open(
                PasswordResetActivity::class.java,
                false
            )
        }
    }

    private fun signInWithGoogle() {
        if (AuthHelper.instance.googleSignInRunning) {
            return
        }
        lifecycleScope.launch {
            viewModel.signInWithGoogle { error -> showModalAlert(getString(R.string.oops), error) }
            updateUiForGoogleSignIn()
        }
    }

    private fun checkAuthState() {
        if (AuthHelper.instance.googleSignInRunning) {
            return
        }

        if (viewModel.isSignedIn) {
            if (viewModel.isEmailVerified) {
                viewModel.hasProfile { profileFound, firebaseUnavailable ->
                    if (firebaseUnavailable) {
                        updateUI(false)
                    } else {
                        open(
                            if (profileFound) HomeActivity::class.java else ProfileSetupActivity::class.java,
                            true
                        )
                    }
                }
            } else {
                open(VerifyEmailActivity::class.java, true)
            }
        }
    }

    private fun updateUiForGoogleSignIn() {
        binding.loginProgressIndicator.visibility = View.GONE
        if (AuthHelper.instance.googleSignInRunning) {
            binding.googleSignInButton.isEnabled = false
            binding.googleSignInProgressIndicator.isEnabled = true
            binding.googleSignInProgressIndicator.visibility = View.VISIBLE
            binding.emailAddressLayout.isEnabled = false
            binding.passwordLayout.isEnabled = false
            binding.resetPasswordButton.isEnabled = false
            binding.loginButton.isEnabled = false
            binding.registerButton.isEnabled = false
        } else {
            binding.googleSignInButton.isEnabled = true
            binding.googleSignInProgressIndicator.isEnabled = false
            binding.googleSignInProgressIndicator.visibility = View.GONE
            binding.emailAddressLayout.isEnabled = true
            binding.passwordLayout.isEnabled = true
            binding.resetPasswordButton.isEnabled = true
            binding.loginButton.isEnabled = true
            binding.registerButton.isEnabled = true
        }
    }

    private fun updateUI(locked: Boolean) {
        binding.googleSignInProgressIndicator.visibility = View.GONE
        if (locked) {
            binding.googleSignInButton.isEnabled = false
            binding.loginProgressIndicator.isEnabled = true
            binding.loginProgressIndicator.visibility = View.VISIBLE
            binding.emailAddressLayout.isEnabled = false
            binding.passwordLayout.isEnabled = false
            binding.resetPasswordButton.isEnabled = false
            binding.loginButton.isEnabled = false
            binding.registerButton.isEnabled = false
        } else {
            binding.googleSignInButton.isEnabled = true
            binding.loginProgressIndicator.isEnabled = false
            binding.loginProgressIndicator.visibility = View.GONE
            binding.emailAddressLayout.isEnabled = true
            binding.passwordLayout.isEnabled = true
            binding.resetPasswordButton.isEnabled = true
            binding.loginButton.isEnabled = true
            binding.registerButton.isEnabled = true
        }
    }

    private fun login() {
        binding.emailAddressLayout.error = null
        binding.passwordLayout.error = null
        val emailAddress = binding.emailAddressTextInput.editableText?.toString()?.trim() ?: ""
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.error = getText(R.string.email_address_is_required)
            return
        }
        val password = binding.passwordTextInput.editableText?.toString()?.trim() ?: ""
        if (password.isEmpty()) {
            binding.passwordLayout.error = getText(R.string.password_is_required)
            return
        }

        updateUI(true)
        viewModel.signIn(emailAddress, password, this::onLoginCompleted)
    }

    private fun onLoginCompleted(error: String) {
        updateUI(false)
        val message = error.trim()
        if (message.isEmpty()) {
            checkAuthState()
        } else {
            showModalAlert(getString(R.string.oops), message)
        }
    }
}