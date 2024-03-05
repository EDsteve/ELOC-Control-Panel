package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityLoginBinding
import de.eloc.eloc_control_panel.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher
import de.eloc.eloc_control_panel.interfaces.VoidCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : NetworkMonitoringActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val authHelper = AuthHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInLayout.visibility = View.GONE
        binding.checkInternetAccessProgressIndicator.visibility = View.VISIBLE
        networkChangedHandler = VoidCallback {
            binding.signInLayout.visibility = View.GONE
            binding.checkInternetAccessProgressIndicator.visibility = View.GONE
            binding.offlineLayout.visibility = View.GONE
            when (hasInternetAccess) {
                true -> binding.signInLayout.visibility = View.VISIBLE
                false -> binding.offlineLayout.visibility = View.VISIBLE
                null -> binding.checkInternetAccessProgressIndicator.visibility = View.VISIBLE
            }
        }

        authHelper.registerGoogleSignInListener(::updateUiForGoogleSignIn)
        setListeners()
        setTextWatchers()
        updateUiForGoogleSignIn()
        updateUI(false)
    }

    override fun onResume() {
        super.onResume()
        onNetworkChanged()
        if (hasInternetAccess != null) {
            val autoGoogleSignIn = PreferencesHelper.instance.getAutoGoogleSignIn()
            val googleSignInCanceled = authHelper.googleSignInCanceled
            if (autoGoogleSignIn && (!googleSignInCanceled)) {
                signInWithGoogle(true)
            } else {
                checkAuthState()
            }
        }
    }

    private fun setTextWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
    }

    private fun setListeners() {
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle(true)
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

    private fun signInWithGoogle(filter: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            authHelper.clearGoogleSignInCanceled()
            authHelper.signInWithGoogle(this@LoginActivity, filter, ::handleGoogleSignIn)
            withContext(Dispatchers.Main) {
                updateUiForGoogleSignIn()
            }
        }
    }

    private fun handleGoogleSignIn(signedIn: Boolean, filtered: Boolean, errorMessage: String) {
        runOnUiThread {
            if (signedIn) {
                checkAuthState()
            } else if (filtered) {
                signInWithGoogle(false)
            } else {
                showModalAlert(getString(R.string.oops), errorMessage)
            }
        }
    }

    private fun checkAuthState() {
        if (authHelper.googleSignInRunning) {
            return
        }

        if (authHelper.isSignedIn) {
            if (authHelper.isEmailAddressVerified) {
                FirestoreHelper.instance.hasProfile(authHelper.userId) { profileFound, firebaseUnavailable ->
                    if (firebaseUnavailable) {
                        updateUI(false)
                    } else {
                        open(
                            if (profileFound) LoadProfileActivity::class.java else ProfileSetupActivity::class.java,
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
        runOnUiThread {
            binding.loginProgressIndicator.visibility = View.GONE
            if (authHelper.googleSignInRunning) {
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
        authHelper.signIn(emailAddress, password, this::onLoginCompleted)
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