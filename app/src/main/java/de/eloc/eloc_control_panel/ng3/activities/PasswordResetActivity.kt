package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import android.view.View

import androidx.lifecycle.ViewModelProvider

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import de.eloc.eloc_control_panel.databinding.ActivityPasswordResetBinding
import de.eloc.eloc_control_panel.ng2.activities.TextInputWatcher

class PasswordResetActivity : ThemableActivity() {
    private lateinit var binding: ActivityPasswordResetBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordResetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        updateUI(false)
        setListeners()
    }

    private fun setListeners() {
        binding.backButton.setOnClickListener { goBack() }
        binding.sendButton.setOnClickListener { sendResetLink() }
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.root.setOnClickListener { hideKeyboard() }
    }

    private fun updateUI(locked: Boolean) {
        binding.emailAddressLayout.isEnabled = !locked
        binding.sendButton.isEnabled = !locked
        binding.progressIndicator.visibility = if (locked) View.VISIBLE else View.GONE
    }

    private fun sendResetLink() {
        binding.emailAddressLayout.error = null
        val emailAddress = binding.emailAddressTextInput.editableText?.toString()?.trim() ?: ""
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.error = getText(R.string.email_address_is_required)
            return
        }
        updateUI(true)
        viewModel.sendPasswordResetLink(emailAddress, this::onResult)
    }

    private fun onResult(sent: Boolean) {
        updateUI(false)
        if (sent) {
            showModalAlert(
                getString(R.string.link_sent),
                getString(R.string.reset_link_sent_message),
                this::goBack
            )
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.send_reset_link_error)
            )
        }
    }
}