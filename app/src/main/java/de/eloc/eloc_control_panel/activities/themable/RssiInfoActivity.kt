package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.data.RssiLabel
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityRssiInfoBinding

class RssiInfoActivity : ThemableActivity() {
    private lateinit var binding: ActivityRssiInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRssiInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setListeners()
    }

    private fun setListeners() {
        binding.noneItem.setOnClickListener { typeSelected(RssiLabel.None) }
        binding.descriptionItem.setOnClickListener { typeSelected(RssiLabel.DescriptionOnly) }
        binding.powerItem.setOnClickListener { typeSelected(RssiLabel.PowerOnly) }
        binding.descriptionAndPowerItem.setOnClickListener { typeSelected(RssiLabel.DescriptionAndPower) }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun typeSelected(label: RssiLabel) {
        Preferences.rssiLabel = label
        goBack()
    }
}