package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.button.MaterialButton
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.activities.overrideGoBack
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.themable.media.ProfileSetupActivity
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.databinding.ActivityRegisterBinding
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class RegisterActivity : ThemableActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private var isEmailSectionExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        App.instance.addNetworkChangedHandler(localClassName) {
            runOnUiThread {
                binding.registrationScrollView.visibility = View.GONE
                binding.checkInternetAccessProgressIndicator.visibility = View.GONE
                binding.offlineLayout.visibility = View.GONE
                when (App.instance.isOnline()) {
                    true -> binding.registrationScrollView.visibility = View.VISIBLE
                    false -> binding.offlineLayout.visibility = View.VISIBLE
                    null -> binding.checkInternetAccessProgressIndicator.visibility = View.VISIBLE
                }
            }
        }

        setListeners()
        setWatchers()
        updateUI(false)
    }

    override fun onResume() {
        super.onResume()
        App.instance.onNetworkChanged()
    }

    private fun setWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
        binding.verifyPasswordTextInput.addTextChangedListener(TextInputWatcher(binding.verifyPasswordLayout))
    }

    private fun setListeners() {
        binding.backButton.setOnClickListener { goToWelcome() }
        binding.loginButton.setOnClickListener {
            openActivity(LoginActivity::class.java, true)
        }
        binding.registerButton.setOnClickListener { register() }
        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
        binding.emailSignUpButton.setOnClickListener { toggleEmailSection() }
        binding.root.setOnClickListener { hideKeyboard() }
        overrideGoBack { goToWelcome() }
    }

    private fun toggleEmailSection() {
        isEmailSectionExpanded = !isEmailSectionExpanded
        val emailButton = binding.emailSignUpButton as MaterialButton
        if (isEmailSectionExpanded) {
            binding.emailSectionLayout.visibility = View.VISIBLE
            emailButton.icon = AppCompatResources.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            binding.emailSectionLayout.visibility = View.GONE
            emailButton.icon = AppCompatResources.getDrawable(this, R.drawable.keyboard_arrow_down)
            hideKeyboard()
        }
    }

    private fun register() {
        hideKeyboard()
        binding.emailAddressLayout.error = null
        binding.passwordLayout.error = null
        binding.verifyPasswordLayout.error = null
        val emailAddress = binding.emailAddressTextInput.editableText?.toString()?.trim() ?: ""
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.error = getString(R.string.email_address_is_required)
            binding.emailAddressTextInput.requestFocus()
            return
        }

        val passwordMinLength = resources.getInteger(R.integer.password_min_length)
        val password = binding.passwordTextInput.editableText?.toString() ?: ""
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.password_is_required)
            binding.passwordTextInput.requestFocus()
            return
        } else if (password.length < passwordMinLength) {
            binding.passwordLayout.error = getString(R.string.password_too_short)
            binding.passwordTextInput.requestFocus()
            return
        }

        val verify = binding.verifyPasswordTextInput.editableText?.toString() ?: ""
        if (verify != password) {
            binding.verifyPasswordLayout.error = getString(R.string.passwords_must_match)
            binding.verifyPasswordTextInput.requestFocus()
            return
        }

        updateUI(true)
        AuthHelper.instance.register(emailAddress, password, ::registrationHandler)
    }

    private fun registrationHandler(error: String) {
        updateUI(false)
        val err = error.trim()
        if (err.isEmpty()) {
            openActivity(VerifyEmailActivity::class.java, true)
        } else {
            showModalAlert(getString(R.string.oops), err)
        }
    }

    private fun updateUI(locked: Boolean) {
        binding.progressIndicator.visibility = if (locked) View.VISIBLE else View.INVISIBLE
        binding.emailAddressLayout.isEnabled = !locked
        binding.passwordLayout.isEnabled = !locked
        binding.verifyPasswordLayout.isEnabled = !locked
        binding.registerButton.isEnabled = !locked
        binding.googleSignInButton.isEnabled = !locked
        binding.emailSignUpButton.isEnabled = !locked
        binding.loginButton.isEnabled = !locked
    }

    private fun signInWithGoogle() {
        updateUI(true)
        AuthHelper.instance.signInWithGoogle(this) { signedIn, _, error ->
            runOnUiThread {
                updateUI(false)
                if (signedIn) {
                    checkAuthStateForGoogleSignIn()
                } else if (error.isNotEmpty()) {
                    showModalAlert(getString(R.string.oops), error)
                }
            }
        }
    }

    private fun checkAuthStateForGoogleSignIn() {
        // Google accounts are automatically verified
        val authHelper = AuthHelper.instance
        if (authHelper.isSignedIn) {
            FirestoreHelper.instance.hasProfile(authHelper.userId) { profileFound, firebaseUnavailable ->
                if (firebaseUnavailable) {
                    updateUI(false)
                } else {
                    val target =
                        if (profileFound) LoadProfileActivity::class.java else ProfileSetupActivity::class.java
                    openActivity(target, finishTask = true)
                }
            }
        }
    }
}
