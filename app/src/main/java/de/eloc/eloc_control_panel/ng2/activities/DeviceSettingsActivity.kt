package de.eloc.eloc_control_panel.ng2.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.TerminalActivity
import de.eloc.eloc_control_panel.databinding.ActivityDeviceSettingsBinding
import de.eloc.eloc_control_panel.ng2.models.GainType
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper
import java.lang.NumberFormatException

class DeviceSettingsActivity : ThemableActivity() {
    private lateinit var binding: ActivityDeviceSettingsBinding
    private val preferencesManager = PreferencesHelper.instance
    private var paused = true
    private var deviceName = ""
    private var sampleRate = 0
    private var secondsPerFile = 0
    private var location = ""
    private var micGain = GainType.High

    private companion object {
        const val COMMAND = "command"
        private const val SEPARATOR = "#"
        private const val COMMAND_PREFIX = SEPARATOR + "settings" + SEPARATOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setDeviceName()
        setData()
        setMicData()
        setToolbar()
        setListeners()
        hideAdvancedOperations()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        val savedBtRecordingState = preferencesManager.getBluetoothRecordingState()
        binding.btRecordingStateButton.isChecked = savedBtRecordingState
        paused = false
    }

    private fun setDeviceName() {
        val extras = intent.extras
        deviceName = extras?.getString(TerminalActivity.EXTRA_DEVICE_NAME, "") ?: ""
        if (deviceName.isNotEmpty()) {
            binding.deviceNameEditText.setText(deviceName.trim())
        }
    }

    private fun setData() {
        val data = preferencesManager.getDeviceSettings()
        //gInitialSettings=true;
        //
        // #16000 sample rate /0
        // #10 seconds /1
        // #loc /2
        // First item (0) is not used, so firmware dev was counting from 1, not 0.
        val separated = data.split(SEPARATOR)
        if (separated.size < 4) {
            return
        }

        var sampleRateObject: Int? = null
        try {
            sampleRateObject = separated[0].toInt()
        } catch (_: NumberFormatException) {
        }
        sampleRate = sampleRateObject ?: 16000
        if (sampleRate <= 8000) {
            binding.rad8k.isChecked = true
        } else if (sampleRate <= 16000) {
            binding.rad16k.isChecked = true
        } else if (sampleRate <= 22050) {
            binding.rad22k.isChecked = true
        } else if (sampleRate <= 32000) {
            binding.rad32k.isChecked = true
        } else if (sampleRate <= 44100) {
            binding.rad44k.isChecked = true
        }

        var secondsObject: Int? = null
        try {
            secondsObject = separated[2].toInt()
        } catch (_: NumberFormatException) {
        }
        secondsPerFile = secondsObject ?: 3600
        if (secondsPerFile <= 10) {
            binding.rad10s.isChecked = true
        } else if (secondsPerFile <= 60) {
            binding.rad1m.isChecked = true
        } else if (secondsPerFile <= 3600) {
            binding.rad1h.isChecked = true
        } else if (secondsPerFile <= 14400) {
            binding.rad4h.isChecked = true
        } else if (secondsPerFile <= 43200) {
            binding.rad12h.isChecked = true
        }

        location = separated[3].trim()
        binding.elocBtNameEditText.setText(location)
    }

    private fun setMicData() {
        val data = preferencesManager.getMicData()
        val separated = data.split("#")
        if (separated.size < 2) {
            return
        }

        binding.micTypeEditText.setText(separated[1].trim())

        var gainObject: Int? = null
        try {
            gainObject = separated[2].trim().toInt()
        } catch (_: NumberFormatException) {
        }
        micGain = GainType.fromValue(gainObject)
        when (micGain) {
            GainType.Low -> binding.radLow.isChecked = true
            GainType.High -> binding.radHigh.isChecked = true
        }
    }

    private fun setToolbar() {
        supportActionBar?.title = getString(R.string.eloc_device_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setListeners() {
        binding.sampleRateRadioGroup.setOnCheckedChangeListener { _, id -> sampleRateChanged(id) }
        binding.secondsPerFileRadioGroup.setOnCheckedChangeListener { _, id -> secondsPerFileChanged(id) }
        binding.gainRadioGroup.setOnCheckedChangeListener { _, id -> gainChanged(id) }
        binding.commandLineButton.setOnClickListener { runCommandLine() }
        binding.recordingButton.setOnClickListener { runRecordingCommand() }
        binding.microphoneTypeButton.setOnClickListener { runMicTypeCommand() }
        binding.microphoneGainButton.setOnClickListener { runMicGainCommand() }
        binding.fileHeaderButton.setOnClickListener { runFileHeaderCommand() }
        binding.hideAdvancedOptionsButton.setOnClickListener { hideAdvancedOperations() }
        binding.showAdvancedOptionsButton.setOnClickListener { showAdvancedOperations() }
        binding.instructionsButton.setOnClickListener { ActivityHelper.showInstructions(this) }
        binding.updateFirmwareButton.setOnClickListener { confirmFirmwareUpdate() }
        binding.btRecordingStateButton.setOnCheckedChangeListener { _, b -> toggleBtRecordingState(b) }
    }

    private fun hideAdvancedOperations() {
        toggleAdvancedOperations(false)
    }

    private fun showAdvancedOperations() {
        toggleAdvancedOperations(true)
    }

    private fun toggleAdvancedOperations(show: Boolean) {
        for (i in 0 until binding.cardContainer.childCount) {
            val child = binding.cardContainer.getChildAt(i)
            val id = child.id
            if ((R.id.recording_card == id) || (R.id.instructions_button == id)) {
                continue
            }
            child.visibility = if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        if (show) {
            binding.hideAdvancedOptionsButton.visibility = View.VISIBLE
            binding.showAdvancedOptionsButton.visibility = View.GONE
        } else {
            binding.hideAdvancedOptionsButton.visibility = View.GONE
            binding.showAdvancedOptionsButton.visibility = View.VISIBLE
        }
    }

    private fun sampleRateChanged(id: Int) {
        when (id) {
            R.id.rad8k -> sampleRate = 8000
            R.id.rad16k -> sampleRate = 16000
            R.id.rad22k -> sampleRate = 22050
            R.id.rad32k -> sampleRate = 32000
            R.id.rad44k -> sampleRate = 44100
        }
    }

    private fun secondsPerFileChanged(id: Int) {
        when (id) {
            R.id.rad10s -> secondsPerFile = 10
            R.id.rad1m -> secondsPerFile = 60
            R.id.rad1h -> secondsPerFile = 3600
            R.id.rad4h -> secondsPerFile = 14400
            R.id.rad12h -> secondsPerFile = 43200
        }
    }

    private fun gainChanged(id: Int) {
        when (id) {
            R.id.radHigh -> micGain = GainType.High
            R.id.radLow -> micGain = GainType.Low
        }
    }

    private fun runCommand(command: String) {
        ActivityHelper.hideKeyboard(this)
        val properCommand = if (command.startsWith(COMMAND_PREFIX)) {
            command
        } else {
            COMMAND_PREFIX + command
        }
        val intent = Intent()
        intent.putExtra(COMMAND, properCommand)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun runCommandLine() {
        val command = binding.customCommandEditText.text?.toString()?.trim() ?: ""
        if (command.isEmpty()) {
            JavaActivityHelper.showModalAlert(
                    this,
                    getString(R.string.required),
                    getString(R.string.command_missing),
            )
        } else {
            runCommand(command)
        }
    }

    private fun runRecordingCommand() {
        val regex = "^[a-zA-Z\\d]+$".toRegex()
        location = binding.elocBtNameEditText.text.toString().trim()
        if (location.isEmpty()) {
            JavaActivityHelper.showModalAlert(
                    this,
                    getString(R.string.required),
                    getString(R.string.file_header_missing)
            )
        } else if (!location.matches(regex)) {
            JavaActivityHelper.showModalAlert(
                    this,
                    getString(R.string.invalid),
                    getString(R.string.invalid_file_header_name)
            )
        } else {
            val command = COMMAND_PREFIX + sampleRate + SEPARATOR + secondsPerFile + SEPARATOR + location
            runCommand(command)
        }
    }

    private fun runMicTypeCommand() {
        val type = binding.micTypeEditText.text.toString().trim()
        if (type.isEmpty()) {
            JavaActivityHelper.showModalAlert(
                    this,
                    getString(R.string.required),
                    getString(R.string.mic_type_missing)
            )
        } else {
            val command = getString(R.string.set_mic_type_template, type)
            runCommand(command)
        }
    }

    private fun runMicGainCommand() {
        val command = getString(R.string.set_mic_gain_template, micGain.value)
        runCommand(command)
    }

    private fun runFileHeaderCommand() {
        val name = binding.deviceNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            JavaActivityHelper.showModalAlert(
                    this,
                    getString(R.string.required),
                    getString(R.string.device_name_missing)
            )
        } else {
            val suffix = "setname"
            val command = if (name.endsWith(suffix)) {
                name
            } else {
                name + suffix
            }
            runCommand(command)
        }
    }

    private fun confirmFirmwareUpdate() {
        JavaActivityHelper.showModalOptionAlert(
                this,
                getString(R.string.confirm_update),
                getString(R.string.update_rationale, deviceName)
        ) {
            runCommand("update")
        }
    }

    private fun toggleBtRecordingState(btOn: Boolean) {
        if (!paused) {
            preferencesManager.setBluetoothRecordingState(btOn)
            val intent = Intent()
            val command = if (btOn) {
                COMMAND_PREFIX + "bton"
            } else {
                COMMAND_PREFIX + "btoff"
            }
            intent.putExtra(COMMAND, command)
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}