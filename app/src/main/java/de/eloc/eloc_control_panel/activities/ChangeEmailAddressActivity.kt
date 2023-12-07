package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityChangeEmailAddressBinding
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class ChangeEmailAddressActivity : ThemableActivity() {

    private lateinit var binding: ActivityChangeEmailAddressBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeEmailAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        binding.currentEmailAddressTextView.text = viewModel.emailAddress

        setListeners()
        updateUI(false)
    }

    private fun updateUI(locked: Boolean) {
        if (locked) {
            binding.textEditLayout.isEnabled = false
            binding.passwordLayout.isEnabled = false
            binding.submitButton.isEnabled = false
            binding.progressHorizontal.visibility = View.VISIBLE
        } else {
            binding.textEditLayout.isEnabled = true
            binding.passwordLayout.isEnabled = true
            binding.submitButton.isEnabled = true
            binding.progressHorizontal.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.root.setOnClickListener { hideKeyboard() }
        binding.submitButton.setOnClickListener { submit() }
        binding.editText.addTextChangedListener(TextInputWatcher(binding.textEditLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun submit() {
        hideKeyboard()
        binding.textEditLayout.error = null
        binding.passwordLayout.error = null

        val emailAddress = binding.editText.editableText?.toString()?.trim() ?: ""
        if (emailAddress.isEmpty()) {
            binding.textEditLayout.error = getString(R.string.required)
            return
        }

        val password = binding.passwordTextInput.editableText?.toString() ?: ""
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.required)
            return
        }

        updateUI(true)
        viewModel.changeEmailAddress(emailAddress, password, this::emailAddressSubmitted)
    }

    private fun emailAddressSubmitted(error4: String) {
        val err = error4.trim()
        if (err.isEmpty()) {
            showModalAlert(
                getString(R.string.account),
                getString(R.string.email_address_updated)
            ) { open(VerifyEmailActivity::class.java, true) }
        } else {
            showModalAlert(getString(R.string.oops), err)
            updateUI(false)
        }
    }
}