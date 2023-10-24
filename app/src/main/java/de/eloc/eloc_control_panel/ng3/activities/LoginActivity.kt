package de.eloc.eloc_control_panel.ng3.activities

import android.content.Intent
import android.os.Bundle
import android.view.View

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityLoginBinding
import de.eloc.eloc_control_panel.ng2.activities.HomeActivity
import de.eloc.eloc_control_panel.ng2.activities.ProfileSetupActivity
import de.eloc.eloc_control_panel.ng2.activities.RegisterActivity
import de.eloc.eloc_control_panel.ng2.activities.TextInputWatcher
import de.eloc.eloc_control_panel.ng3.data.UserAccountViewModel

class LoginActivity : ThemableActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var viewModel: UserAccountViewModel
    private var googleSignInActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        setListeners()
        setLaunchers()
        setTextWatchers()
        updateUIForGoogleSignIn()
        updateUI(false)
    }

    override fun onResume() {
        super.onResume()
        checkAuthState(false)
    }

    private fun setTextWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
    }

    private fun setLaunchers() {
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            viewModel.signInWithGoogle(result.data, this::checkAuthState)
        }
    }

    private fun setListeners() {
        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
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
        googleSignInActive = true
        updateUIForGoogleSignIn()
        googleSignInLauncher.launch(viewModel.googleSignInIntent)
    }

    private fun checkAuthState(completingSignIn: Boolean) {
        if (completingSignIn) {
            googleSignInActive = false
        }
        if (googleSignInActive) {
            return
        }

        if (viewModel.isSignedIn) {
            if (viewModel.isEmailVerified) {
                viewModel.hasProfile { profileFound ->
                    open(
                        if (profileFound) HomeActivity::class.java else ProfileSetupActivity::class.java,
                        true
                    )
                }
            } else {
                open(VerifyEmailActivity::class.java, true)
            }
        }
        updateUIForGoogleSignIn()
    }

    private fun updateUIForGoogleSignIn() {
        binding.loginProgressIndicator.visibility = View.GONE
        if (googleSignInActive) {
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
            checkAuthState(false)
        } else {
            showModalAlert(getString(R.string.oops), message)
        }
    }
}