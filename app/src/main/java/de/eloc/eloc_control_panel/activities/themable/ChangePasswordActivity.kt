package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.databinding.ActivityChangePasswordBinding
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showModalAlert

class ChangePasswordActivity : ThemableActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        updateUI(false)
    }

    private fun updateUI(locked: Boolean) {
        if (locked) {
            binding.currentPasswordLayout.isEnabled = false
            binding.newPasswordLayout.isEnabled = false
            binding.verifyPasswordLayout.isEnabled = false
            binding.submitButton.isEnabled = false
            binding.progressIndicator.visibility = View.VISIBLE
        } else {
            binding.currentPasswordLayout.isEnabled = true
            binding.newPasswordLayout.isEnabled = true
            binding.verifyPasswordLayout.isEnabled = true
            binding.submitButton.isEnabled = true
            binding.progressIndicator.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.root.setOnClickListener { hideKeyboard() }
        binding.submitButton.setOnClickListener { submit() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
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
        AuthHelper.instance.changePassword(newPassword, currentPassword, ::newPasswordSubmitted)
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