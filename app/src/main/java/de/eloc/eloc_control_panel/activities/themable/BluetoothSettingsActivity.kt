package de.eloc.eloc_control_panel.activities.themable

import android.os.Build
import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.databinding.ActivityBluetoothSettingsBinding
import de.eloc.eloc_control_panel.activities.open
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.data.util.Preferences

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

        // CompanionDeviceManager is only available after Oreo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            binding.associatedDevicesItem.visibility = View.GONE
            binding.divider2.visibility = View.GONE
        }

        binding.associatedDevicesItem.setOnClickListener {
            open(ManageAssociationsActivity::class.java)
        }
        binding.signalInfoItem.setOnClickListener {
            open(RssiInfoActivity::class.java)
        }
        binding.showAllDevicesItem.setSwitchClickedListener {
            Preferences.showAllBluetoothDevices = binding.showAllDevicesItem.isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        binding.signalInfoItem.valueText = Preferences.rssiLabel.text
        binding.showAllDevicesItem.setSwitch(Preferences.showAllBluetoothDevices)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }
}