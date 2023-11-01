package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityRegisterBinding
import de.eloc.eloc_control_panel.ng2.activities.TextInputWatcher
import de.eloc.eloc_control_panel.ng3.data.UserAccountViewModel

class RegisterActivity : ThemableActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]

        setListeners()
        setWatchers()
        updateUI(false)
    }

    private fun setWatchers() {
        binding.emailAddressTextInput.addTextChangedListener(TextInputWatcher(binding.emailAddressLayout))
        binding.passwordTextInput.addTextChangedListener(TextInputWatcher(binding.passwordLayout))
        binding.verifyPasswordTextInput.addTextChangedListener(TextInputWatcher(binding.verifyPasswordLayout))
    }

    private fun setListeners() {
        binding.backButton.setOnClickListener { openLogin() }
        binding.loginButton.setOnClickListener { openLogin() }
        binding.registerButton.setOnClickListener { register() }
        binding.root.setOnClickListener { hideKeyboard() }
        overrideGoBack { openLogin() }
    }

    private fun openLogin() = open(LoginActivity::class.java, true)

    private fun register() {
        hideKeyboard()
        binding.emailAddressLayout.error = null
        binding.passwordLayout.error = null
        binding.verifyPasswordLayout.error = null
        val emailAddress = binding.emailAddressTextInput.editableText?.toString()?.trim() ?: ""
        if (emailAddress.isEmpty()) {
            binding.emailAddressLayout.error = getString(R.string.email_address_is_required)
            binding.emailAddressTextInput.requestFocus()
            return
        }

        val passwordMinLength = resources.getInteger(R.integer.password_min_length)
        val password = binding.passwordTextInput.editableText?.toString() ?: ""
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.password_is_required)
            binding.passwordTextInput.requestFocus()
            return
        } else if (password.length < passwordMinLength) {
            binding.passwordLayout.error = getString(R.string.password_too_short)
            binding.passwordTextInput.requestFocus()
            return
        }

        val verify = binding.verifyPasswordTextInput.editableText?.toString() ?: ""
        if (verify != password) {
            binding.verifyPasswordLayout.error = getString(R.string.passwords_must_match)
            binding.verifyPasswordTextInput.requestFocus()
            return
        }

        updateUI(true)
        viewModel.register(emailAddress, password, this::registrationHandler)
    }

    private fun registrationHandler(error: String) {
        updateUI(false)
        val err = error.trim()
        if (err.isEmpty()) {
            open(VerifyEmailActivity::class.java, true)
        } else {
            showModalAlert(getString(R.string.oops), err)
        }
    }

    private fun updateUI(locked: Boolean) {
        binding.progressHorizontal.visibility = if (locked) View.VISIBLE else View.INVISIBLE
        binding.emailAddressLayout.isEnabled = !locked
        binding.passwordLayout.isEnabled = !locked
        binding.verifyPasswordLayout.isEnabled = !locked
        binding.registerButton.isEnabled = !locked
    }
}