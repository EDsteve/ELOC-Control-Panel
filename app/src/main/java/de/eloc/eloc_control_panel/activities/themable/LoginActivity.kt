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
import de.eloc.eloc_control_panel.databinding.ActivityLoginBinding
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class LoginActivity : ThemableActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val authHelper = AuthHelper.instance
    private var isEmailSectionExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInScrollView.visibility = View.GONE
        binding.checkInternetAccessProgressIndicator.visibility = View.VISIBLE
        App.instance.addNetworkChangedHandler(this.localClassName) {
            runOnUiThread {
                binding.signInScrollView.visibility = View.GONE
                binding.checkInternetAccessProgressIndicator.visibility = View.GONE
                binding.offlineLayout.visibility = View.GONE
                when (App.instance.isOnline()) {
                    true -> binding.signInScrollView.visibility = View.VISIBLE
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
        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
        binding.emailSignInButton.setOnClickListener { toggleEmailSection() }
        overrideGoBack {
            goToWelcome()
        }
    }

    private fun toggleEmailSection() {
        isEmailSectionExpanded = !isEmailSectionExpanded
        val emailButton = binding.emailSignInButton as MaterialButton
        if (isEmailSectionExpanded) {
            binding.emailSectionLayout.visibility = View.VISIBLE
            emailButton.icon = AppCompatResources.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            binding.emailSectionLayout.visibility = View.GONE
            emailButton.icon = AppCompatResources.getDrawable(this, R.drawable.keyboard_arrow_down)
            hideKeyboard()
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
            binding.googleSignInButton.isEnabled = false
            binding.emailSignInButton.isEnabled = false
        } else {
            binding.loginProgressIndicator.isEnabled = false
            binding.loginProgressIndicator.visibility = View.GONE
            binding.emailAddressLayout.isEnabled = true
            binding.passwordLayout.isEnabled = true
            binding.resetPasswordButton.isEnabled = true
            binding.loginButton.isEnabled = true
            binding.registerButton.isEnabled = true
            binding.googleSignInButton.isEnabled = true
            binding.emailSignInButton.isEnabled = true
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

    private fun signInWithGoogle() {
        updateUI(true)
        authHelper.signInWithGoogle(this) { signedIn, _, error ->
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
