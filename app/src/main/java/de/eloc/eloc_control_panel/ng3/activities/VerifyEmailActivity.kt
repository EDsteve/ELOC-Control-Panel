package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import android.view.View

import androidx.lifecycle.ViewModelProvider

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import de.eloc.eloc_control_panel.databinding.ActivityVerifyEmailBinding
import de.eloc.eloc_control_panel.ng2.activities.HomeActivity
import de.eloc.eloc_control_panel.ng2.activities.NoActionBarActivity
import de.eloc.eloc_control_panel.ng2.activities.ProfileSetupActivity

class VerifyEmailActivity : NoActionBarActivity() {
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
        binding.verifiedButton.setOnClickListener { signIn() }
        binding.resendButton.setOnClickListener { viewModel.sendEmailVerificationLink(this::onResendCompleted) }
        binding.signOutButton.setOnClickListener {
            viewModel.signOut()
            open(LoginActivity::class.java, true)
        }
    }

    private fun updateUI(showProgress: Boolean) {
        if (showProgress) {
            binding.progressHorizontal.visibility = View.VISIBLE
            binding.messageLayout.visibility = View.GONE
        } else {
            binding.progressHorizontal.visibility = View.GONE
            binding.messageLayout.visibility = View.VISIBLE
        }
    }

    private fun onHasProfileCompleted(hasProfile: Boolean) {
        val target = if (hasProfile) HomeActivity::class.java else ProfileSetupActivity::class.java
        open(target, true)
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
        showModalAlert(title, message) {
            viewModel.signOut()
            open(LoginActivity::class.java, true)
        }
    }

    private fun checkAuthState() {
        updateUI(true)
        if (viewModel.isSignedIn) {
            if (viewModel.isEmailVerified) {
                viewModel.hasProfile(this::onHasProfileCompleted)
            } else {
                val message = getString(R.string.verification_message, viewModel.emailAddress)
                binding.messageTextView.text = message
                updateUI(false)
            }
        } else {
            open(LoginActivity::class.java, true)
        }
    }
}