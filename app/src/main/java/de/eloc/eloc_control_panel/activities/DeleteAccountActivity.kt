package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.StorageHelper
import de.eloc.eloc_control_panel.databinding.ActivityDeleteAccountBinding
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class DeleteAccountActivity : ThemableActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private val authHelper = AuthHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        updateUI(false)
    }

    private fun updateUI(locked: Boolean) {
        if (locked) {
            binding.passwordLayout.isEnabled = false
            binding.submitButton.isEnabled = false
            binding.progressIndicator.visibility = View.VISIBLE
        } else {
            binding.passwordLayout.isEnabled = true
            binding.submitButton.isEnabled = true
            binding.progressIndicator.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.root.setOnClickListener { hideKeyboard() }
        binding.submitButton.setOnClickListener { submit() }
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun submit() {
        hideKeyboard()
        binding.passwordLayout.error = null

        val password = binding.passwordTextInput.editableText?.toString() ?: ""
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.required)
            return
        }

        updateUI(true)
        authHelper.reauthenticate(password, this::onPasswordVerificationCompleted)
    }

    private fun onPasswordVerificationCompleted(err: String) {
        val error = err.trim()
        if (error.isEmpty()) {
            StorageHelper.instance.deleteAccount(authHelper.userId, ::onRemoteFilesDeleted)
        } else {
            showModalAlert(getString(R.string.account), error) { updateUI(false) }
        }
    }

    private fun onRemoteFilesDeleted(success: Boolean) {
        if (success) {
            FirestoreHelper.instance.deleteProfile(authHelper.userId, ::onProfileDeleted)
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.failed_to_remove_remote_files)
            ) { updateUI(false) }
        }
    }

    private fun onProfileDeleted(success: Boolean) {
        if (success) {
            authHelper.deleteAccount(::onAuthAccountDeleted)
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.failed_to_clear_profile),
            ) { updateUI(false) }
        }
    }

    private fun onAuthAccountDeleted(success: Boolean) {
        if (success) {
            showModalAlert(
                getString(R.string.account),
                getString(R.string.your_account_has_been_deleted)
            ) { authHelper.signOut(this@DeleteAccountActivity) }
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.failed_to_remove_account),
            ) { updateUI(false) }
        }
    }
}