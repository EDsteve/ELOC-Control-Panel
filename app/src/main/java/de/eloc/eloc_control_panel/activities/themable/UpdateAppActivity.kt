package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityUpdateAppBinding
import de.eloc.eloc_control_panel.activities.openUrl

class UpdateAppActivity : ThemableActivity() {
    private lateinit var binding : ActivityUpdateAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.updateButton.setOnClickListener {
            openUrl(getString(R.string.store_url))
        }
    }
}