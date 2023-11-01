package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityDeleteAccountBinding
import de.eloc.eloc_control_panel.ng2.activities.TextInputWatcher
import de.eloc.eloc_control_panel.ng3.data.UserAccountViewModel

class DeleteAccountActivity : ThemableActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]

        setListeners()
        updateUI(false)
    }

    private fun updateUI(locked: Boolean) {
        if (locked) {
            binding.passwordLayout.isEnabled = false
            binding.submitButton.isEnabled = false
            binding.progressHorizontal.visibility = View.VISIBLE
        } else {
            binding.passwordLayout.isEnabled = true
            binding.submitButton.isEnabled = true
            binding.progressHorizontal.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.root.setOnClickListener { hideKeyboard() }
        binding.submitButton.setOnClickListener { submit() }
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
        binding.elocAppBar.setOnBackButtonClickedListener { goBack() }
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
        viewModel.verifyPassword(password, this::onPasswordVerificationCompleted)
    }

    private fun onPasswordVerificationCompleted(err: String) {
        val error = err.trim()
        if (error.isEmpty()) {
            viewModel.deleteRemoteFiles(this::onRemoteFilesDeleted)
        } else {
            showModalAlert(getString(R.string.account), error) { updateUI(false) }
        }
    }

    private fun onRemoteFilesDeleted(success: Boolean) {
        if (success) {
            viewModel.deleteProfile(this::onProfileDeleted)
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.failed_to_remove_remote_files)
            ) { updateUI(false) }
        }
    }

    private fun onProfileDeleted(success: Boolean) {
        if (success) {
            viewModel.deleteAuthAccount(this::onAuthAccountDeleted)
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
            ) {
                viewModel.signOut()
                open(LoginActivity::class.java, true)
            }
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.failed_to_remove_account),
            ) { updateUI(false) }

        }
    }
}