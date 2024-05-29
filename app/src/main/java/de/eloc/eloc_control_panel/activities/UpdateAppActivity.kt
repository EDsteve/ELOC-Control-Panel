package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityUpdateAppBinding

class UpdateAppActivity : AppCompatActivity() {
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