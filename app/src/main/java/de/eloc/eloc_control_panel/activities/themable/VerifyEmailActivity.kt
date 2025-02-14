package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.themable.media.ProfileSetupActivity
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.databinding.ActivityVerifyEmailBinding

private enum class UiState {
    Offline, InProgress, Idle
}

class VerifyEmailActivity : ThemableActivity() {
    private lateinit var binding: ActivityVerifyEmailBinding
    private val authHelper = AuthHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        checkAuthState()
    }

    private fun setListeners() {
        binding.retryButton.setOnClickListener { checkAuthState() }
        binding.verifiedButton.setOnClickListener { signIn() }
        binding.resendButton.setOnClickListener {
            authHelper.sendVerificationLink(::onResendCompleted)
        }
        binding.signOutButton.setOnClickListener { signOut() }
    }

    private fun signOut() = authHelper.signOut(this@VerifyEmailActivity)

    private fun updateUI(state: UiState) {
        binding.progressIndicator.visibility = View.GONE
        binding.messageLayout.visibility = View.GONE
        binding.offlineLayoutContainer.visibility = View.GONE
        when (state) {
            UiState.InProgress -> {
                binding.progressIndicator.visibility = View.VISIBLE
            }

            UiState.Idle -> {
                binding.messageLayout.visibility = View.VISIBLE
            }

            UiState.Offline -> {
                binding.offlineLayoutContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun onResendCompleted(sent: Boolean) {
        val (title, message) = if (sent) {
            (getString(R.string.link_sent) to getString(R.string.resend_message))
        } else {
            (getString(R.string.oops) to
                    getString(R.string.resend_failed))
        }
        showModalAlert(title, message)
    }

    private fun signIn() {
        val title = getString(R.string.email_verification)
        val message = getString(R.string.redirect_to_sign_in)
        showModalAlert(title, message) { signOut() }
    }

    private fun checkAuthState() {
        updateUI(UiState.InProgress)
        if (AuthHelper.instance.isSignedIn) {
            if (AuthHelper.instance.isEmailAddressVerified) {
                FirestoreHelper.instance.hasProfile(authHelper.userId)
                { hasProfile, firebaseUnavailable ->
                    if (firebaseUnavailable) {
                        if (hasProfile) {
                            val bundle = Bundle()
                            bundle.putBoolean(LoadProfileActivity.EXTRA_IS_OFFLINE_MODE, true)
                            openActivity(LoadProfileActivity::class.java, true, bundle)
                        } else {
                            updateUI(UiState.Offline)
                        }
                    } else {
                        val target =
                            if (hasProfile) LoadProfileActivity::class.java else ProfileSetupActivity::class.java
                        openActivity(target, true)
                    }
                }
            } else {
                val message =
                    getString(R.string.verification_message, AuthHelper.instance.emailAddress)
                binding.messageTextView.text = message
                updateUI(UiState.Idle)
            }
        } else {
            openActivity(WelcomeActivity::class.java, true)
        }
    }
}