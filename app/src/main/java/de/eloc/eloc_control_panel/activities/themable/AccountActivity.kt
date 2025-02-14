package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.databinding.ActivityAccountBinding

class AccountActivity : ThemableActivity() {
    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setItemTitles()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        setItemValues()
    }

    private fun setItemTitles() {
        binding.changeEmailAddressItem.titleTextView.setText(R.string.email_address)
        binding.changePasswordItem.titleTextView.setText(R.string.password)
        binding.changePasswordItem.valueTextView.setText(R.string.change)
        binding.deleteAccountItem.titleTextView.setText(R.string.delete_account)
    }

    private fun setItemValues() {
        binding.changeEmailAddressItem.valueTextView.text = AuthHelper.instance.emailAddress
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.changeEmailAddressItem.button.setOnClickListener { openEmailAddressEditor() }
        binding.changePasswordItem.button.setOnClickListener { openPasswordEditor() }
        binding.deleteAccountItem.button.setOnClickListener { openAccountDeleter() }
    }

    private fun openEmailAddressEditor() = openActivity(ChangeEmailAddressActivity::class.java)

    private fun openPasswordEditor() = openActivity(ChangePasswordActivity::class.java)

    private fun openAccountDeleter() = openActivity(DeleteAccountActivity::class.java)

}