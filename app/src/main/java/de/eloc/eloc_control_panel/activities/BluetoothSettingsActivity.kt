package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.databinding.ActivityBluetoothSettingsBinding

class BluetoothSettingsActivity : ThemableActivity() {
    private lateinit var binding: ActivityBluetoothSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        binding.systemSettingsItem.setOnClickListener {
            BluetoothHelper.openSettings(this@BluetoothSettingsActivity)
        }
        binding.associatedDevicesItem.setOnClickListener {
            open(ManageAssociationsActivity::class.java)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }
}