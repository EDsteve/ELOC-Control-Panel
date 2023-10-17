package de.eloc.eloc_control_panel.ng3.activities

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator

import de.eloc.eloc_control_panel.databinding.ActivityHomeNg3Binding
import de.eloc.eloc_control_panel.ng3.adapters.HomePagerAdapter

class HomeActivityNg3 : AppCompatActivity() {
    lateinit var binding: ActivityHomeNg3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeNg3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        setListeners()
        setPager()
        setUiData()
    }

    private fun setPager() {
        val adapter = HomePagerAdapter(this)
        binding.pager.adapter = adapter
        TabLayoutMediator(binding.tablayout, binding.pager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.attach()
    }

    private fun setListeners() {
        binding.settingsButton.setOnClickListener {
            println("Open settings")
        }
    }

    private fun setUiData() {
        binding.elocNameTextView.text = "ELOC45"
    }
}