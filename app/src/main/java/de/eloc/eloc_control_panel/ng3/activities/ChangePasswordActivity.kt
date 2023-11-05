package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityChangePasswordBinding
import de.eloc.eloc_control_panel.ng3.data.UserAccountViewModel
import de.eloc.eloc_control_panel.ng3.interfaces.TextInputWatcher

class ChangePasswordActivity : ThemableActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        setListeners()
        updateUI(false)
    }

    private fun updateUI(locked: Boolean) {
        if (locked) {
            binding.currentPasswordLayout.isEnabled = false
            binding.newPasswordLayout.isEnabled = false
            binding.verifyPasswordLayout.isEnabled = false
            binding.submitButton.isEnabled = false
            binding.progressHorizontal.visibility = View.VISIBLE
        } else {
            binding.currentPasswordLayout.isEnabled = true
            binding.newPasswordLayout.isEnabled = true
            binding.verifyPasswordLayout.isEnabled = true
            binding.submitButton.isEnabled = true
            binding.progressHorizontal.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.root.setOnClickListener { hideKeyboard() }
        binding.submitButton.setOnClickListener { submit() }
        binding.elocAppBar.setOnBackButtonClickedListener { goBack() }
        binding.currentPasswordTextInput.addTextChangedListener(TextInputWatcher(binding.currentPasswordLayout))
        binding.newPasswordTextInput.addTextChangedListener(TextInputWatcher(binding.newPasswordLayout))
        binding.verifyPasswordTextInput.addTextChangedListener(TextInputWatcher(binding.verifyPasswordLayout))
        binding.verifyPasswordTextInput.addTextChangedListener(TextInputWatcher(binding.newPasswordLayout))
    }

    private fun submit() {
        hideKeyboard()
        binding.currentPasswordLayout.error = null
        binding.newPasswordLayout.error = null
        binding.verifyPasswordLayout.error = null

        val currentPassword = binding.currentPasswordTextInput.editableText?.toString() ?: ""
        if (currentPassword.isEmpty()) {
            binding.currentPasswordLayout.error = getString(R.string.required)
            return
        }

        val passwordMinLength = resources.getInteger(R.integer.password_min_length)
        val newPassword = binding.newPasswordTextInput.editableText?.toString() ?: ""
        if (newPassword.isEmpty()) {
            binding.newPasswordLayout.error = getString(R.string.required)
            return
        } else if (newPassword.length < passwordMinLength) {
            binding.newPasswordLayout.error = getString(R.string.password_too_short)
            return
        }

        val verifyPassword = binding.verifyPasswordTextInput.editableText?.toString() ?: ""
        if (newPassword != verifyPassword) {
            binding.newPasswordLayout.error = getString(R.string.passwords_must_match)
            return
        }

        updateUI(true)
        viewModel.changePassword(newPassword, currentPassword, this::newPasswordSubmitted)
    }

    private fun newPasswordSubmitted(error: String) {
        val err = error.trim()
        if (err.isEmpty()) {
            showModalAlert(
                getString(R.string.account),
                getString(R.string.password_updated),
                this::goBack,
            )
        } else {
            showModalAlert(getString(R.string.oops), err)
            updateUI(false)
        }
    }
}