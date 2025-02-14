package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.activities.overrideGoBack
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.themable.media.ProfileSetupActivity
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.databinding.ActivityLoginBinding
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class LoginActivity : ThemableActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val authHelper = AuthHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInLayout.visibility = View.GONE
        binding.checkInternetAccessProgressIndicator.visibility = View.VISIBLE
        App.instance.addNetworkChangedHandler(this.localClassName) {
            runOnUiThread {
                binding.signInLayout.visibility = View.GONE
                binding.checkInternetAccessProgressIndicator.visibility = View.GONE
                binding.offlineLayout.visibility = View.GONE
                when (App.instance.isOnline()) {
                    true -> binding.signInLayout.visibility = View.VISIBLE
                    false -> binding.offlineLayout.visibility = View.VISIBLE
                    null -> binding.checkInternetAccessProgressIndicator.visibility = View.VISIBLE
                }
            }
        }

        setListeners()
        setTextWatchers()
        updateUI(false)
    }

    override fun onResume() {
        super.onResume()
        App.instance.onNetworkChanged()
        if (App.instance.isOnline() != null) {
            checkAuthState()
        }
    }

    private fun setTextWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
    }

    private fun setListeners() {
        binding.registerButton.setOnClickListener {
            openActivity(
                RegisterActivity::class.java,
                true
            )
        }
        binding.backButton.setOnClickListener { goToWelcome() }
        binding.loginButton.setOnClickListener { login() }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.resetPasswordButton.setOnClickListener {
            openActivity(PasswordResetActivity::class.java)
        }
        overrideGoBack {
            goToWelcome()
        }
    }

    private fun checkAuthState() {
        if (authHelper.isSignedIn) {
            if (authHelper.isEmailAddressVerified) {
                FirestoreHelper.instance.hasProfile(authHelper.userId) { profileFound, firebaseUnavailable ->
                    if (firebaseUnavailable) {
                        updateUI(false)
                    } else {
                        val target =
                            if (profileFound) LoadProfileActivity::class.java else ProfileSetupActivity::class.java
                        openActivity(target, finishTask = true)
                    }
                }
            } else {
                openActivity(VerifyEmailActivity::class.java, finishTask = true)
            }
        }
    }

    private fun updateUI(locked: Boolean) {
        if (locked) {
            binding.loginProgressIndicator.isEnabled = true
            binding.loginProgressIndicator.visibility = View.VISIBLE
            binding.emailAddressLayout.isEnabled = false
            binding.passwordLayout.isEnabled = false
            binding.resetPasswordButton.isEnabled = false
            binding.loginButton.isEnabled = false
            binding.registerButton.isEnabled = false
        } else {
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