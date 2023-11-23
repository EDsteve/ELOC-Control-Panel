package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityAccountBinding
import de.eloc.eloc_control_panel.ng3.data.UserAccountViewModel
import de.eloc.eloc_control_panel.ng3.data.UserProfile

class AccountActivity : ThemableActivity() {
    private lateinit var binding: ActivityAccountBinding
    private lateinit var viewModel: UserAccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setViewModel()
        setItemTitles()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getProfileAsync()
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        viewModel.profile.observe(this, ::setItemValues)
    }

    private fun setItemTitles() {
        binding.changeEmailAddressItem.titleTextView.setText(R.string.email_address)
        binding.changePasswordItem.titleTextView.setText(R.string.password)
        binding.changePasswordItem.valueTextView.setText(R.string.change)
        binding.deleteAccountItem.titleTextView.setText(R.string.delete_account)
    }

    private fun setItemValues(profile: UserProfile?) {
        binding.changeEmailAddressItem.valueTextView.text = profile?.emailAddress ?: ""
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.changeEmailAddressItem.button.setOnClickListener { openEmailAddressEditor() }
        binding.changePasswordItem.button.setOnClickListener { openPasswordEditor() }
        binding.deleteAccountItem.button.setOnClickListener { openAccountDeleter() }
    }

    private fun openEmailAddressEditor() = open(ChangeEmailAddressActivity::class.java, false)

    private fun openPasswordEditor() = open(ChangePasswordActivity::class.java, false)

    private fun openAccountDeleter() = open(DeleteAccountActivity::class.java, false)

}