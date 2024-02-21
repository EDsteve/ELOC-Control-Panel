package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import android.view.View

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityVerifyEmailBinding
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import kotlinx.coroutines.launch

private enum class UiState {
    Offline, InProgress, Idle
}

class VerifyEmailActivity : ThemableActivity() {
    private lateinit var binding: ActivityVerifyEmailBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        checkAuthState()
    }

    private fun setListeners() {
        binding.retryButton.setOnClickListener { checkAuthState() }
        binding.verifiedButton.setOnClickListener { signIn() }
        binding.resendButton.setOnClickListener { viewModel.sendEmailVerificationLink(this::onResendCompleted) }
        binding.signOutButton.setOnClickListener { signOut() }
    }

    private fun signOut() {
        lifecycleScope.launch {
            viewModel.signOut()
            open(LoginActivity::class.java, true)
        }
    }

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
        if (viewModel.isSignedIn) {
            if (viewModel.isEmailVerified) {
                viewModel.hasProfile { hasProfile, firebaseUnavailable ->
                    if (firebaseUnavailable) {
                        if (hasProfile) {
                            val bundle = Bundle()
                            bundle.putBoolean(HomeActivity.EXTRA_IS_OFFLINE_MODE, true)
                            open(HomeActivity::class.java, true, bundle)
                        } else {
                            updateUI(UiState.Offline)
                        }
                    } else {
                        val target =
                            if (hasProfile) HomeActivity::class.java else ProfileSetupActivity::class.java
                        open(target, true)
                    }
                }
            } else {
                val message = getString(R.string.verification_message, viewModel.emailAddress)
                binding.messageTextView.text = message
                updateUI(UiState.Idle)
            }
        } else {
            open(LoginActivity::class.java, true)
        }
    }
}