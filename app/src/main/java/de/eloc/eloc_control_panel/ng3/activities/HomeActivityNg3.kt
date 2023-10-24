package de.eloc.eloc_control_panel.ng3.activities

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R

import de.eloc.eloc_control_panel.databinding.ActivityHomeNg3Binding
import de.eloc.eloc_control_panel.databinding.LayoutAppBarBinding

class HomeActivityNg3 : AppCompatActivity() {
    private lateinit var binding: ActivityHomeNg3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeNg3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setUiData()
    }

    private fun setUiData() {
        binding.elocAppBar.title = "ChangeMe"
        binding.elocAppBar.userName = "Jane Doe"
        updateBatteryLevel(24.0)
        updateStorageStatus(78.0)
        updateGpsAccuracy(7.0)
        binding.detectingSinceTextView.text = "1D 10H 22M"
        val recording = false
        val recordingColor = if (recording) {
            binding.recordingTextView.setText(R.string.on)
            ContextCompat.getColor(this, R.color.status_field_green)
        } else {
            binding.recordingTextView.setText(R.string.off)
            ContextCompat.getColor(this, R.color.status_field_red)
        }
        binding.recordingTextView.setTextColor(recordingColor)

        val recordWhenDetected = true
        val colorRes = if (recordWhenDetected) {
            binding.recordWhenDetectedTextView.setText(R.string.on)
            R.color.status_field_green
        } else {
            binding.recordWhenDetectedTextView.setText(R.string.off)
            R.color.status_field_red
        }
        val color = ContextCompat.getColor(this, colorRes)
        binding.recordWhenDetectedTextView.setTextColor(color)
        binding.modelTextView.text = "Trumpet_V12"
        binding.communicationTextView.text = "LoRa"
        binding.sampleRateTextView.text = "1600"
        binding.gainTextView.text = "HIGH"
        binding.hoursPerFileTextView.text = "1"
        binding.sessionIdTextView.text = "ELOC234214324"
    }

    private fun updateBatteryLevel(level: Double) {
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batteryGauge.updateValue(level)
    }

    private fun updateStorageStatus(free: Double) {
        binding.storageGauge.updateValue(free)
        binding.storageStatus.text = getString(R.string.gauge_free_space, free.toInt())
    }

    private fun updateGpsAccuracy(meters: Double) {
        binding.gpsGauge.updateValue(meters)
        val res =
            if (meters >= 100) R.string.gauge_gps_long_distance else R.string.gauge_gps_distance
        binding.gpsStatus.text = getString(res, meters.toInt())
    }
}