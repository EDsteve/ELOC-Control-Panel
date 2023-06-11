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
    private var location = ""

    private companion object {
        const val COMMAND = "command"
        private const val SEPARATOR = "#"
        private const val COMMAND_PREFIX = SEPARATOR + "settings" + SEPARATOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setChips()
        setDeviceName()
        setData()
        setMicData()
        setBtState()
        setToolbar()
        setListeners()
        hideAdvancedOperations()
        setChipColors()
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
        val btOnWhenRecording = preferencesManager.getBluetoothRecordingState()
        if (btOnWhenRecording) {
            binding.btOnLayout.chip.isChecked = true
        } else {
            binding.btOffLayout.chip.isChecked = true
        }
        paused = false
    }

    private fun setDeviceName() {
        val extras = intent.extras
        deviceName = extras?.getString(TerminalActivity.EXTRA_DEVICE_NAME, "") ?: ""
        if (deviceName.isNotEmpty()) {
            binding.deviceNameEditText.setText(deviceName.trim())
        }
    }

    private fun setBtState() {
        binding.btOnLayout.chip.isChecked = false
        binding.btOffLayout.chip.isChecked = false
        // todo: must be obtained directly from firmware
        if (preferencesManager.getBluetoothRecordingState()) {
            binding.btOnLayout.chip.isChecked = true
        } else {
            binding.btOffLayout.chip.isChecked = true
        }
        setChipColors()
    }

    private fun setChipColors() {
        ActivityHelper.setChipColors(this, binding.btOnLayout)
        ActivityHelper.setChipColors(this, binding.btOffLayout)

        ActivityHelper.setChipColors(this, binding.lowGainLayout)
        ActivityHelper.setChipColors(this, binding.highGainLayout)

        ActivityHelper.setChipColors(this, binding.time12hLayout)
        ActivityHelper.setChipColors(this, binding.time4hLayout)
        ActivityHelper.setChipColors(this, binding.time1hLayout)
        ActivityHelper.setChipColors(this, binding.time1mLayout)
        ActivityHelper.setChipColors(this, binding.time10sLayout)

        ActivityHelper.setChipColors(this, binding.rate44kLayout)
        ActivityHelper.setChipColors(this, binding.rate32kLayout)
        ActivityHelper.setChipColors(this, binding.rate22kLayout)
        ActivityHelper.setChipColors(this, binding.rate16kLayout)
        ActivityHelper.setChipColors(this, binding.rate8kLayout)
    }

    private fun setChips() {
        binding.btOnLayout.chip.setText(R.string.on)
        binding.btOffLayout.chip.setText(R.string.off)
        binding.lowGainLayout.chip.setText(R.string.low)
        binding.highGainLayout.chip.setText(R.string.high)

        binding.time12hLayout.chip.setText(R.string._12h)
        binding.time4hLayout.chip.setText(R.string._4h)
        binding.time1hLayout.chip.setText(R.string._1h)
        binding.time1mLayout.chip.setText(R.string._1m)
        binding.time10sLayout.chip.setText(R.string._10s)

        binding.rate44kLayout.chip.setText(R.string._44k)
        binding.rate32kLayout.chip.setText(R.string._32k)
        binding.rate22kLayout.chip.setText(R.string._22k)
        binding.rate16kLayout.chip.setText(R.string._16k)
        binding.rate8kLayout.chip.setText(R.string._8k)
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
        binding.rate44kLayout.chip.isChecked = false
        binding.rate32kLayout.chip.isChecked = false
        binding.rate22kLayout.chip.isChecked = false
        binding.rate16kLayout.chip.isChecked = false
        binding.rate8kLayout.chip.isChecked = false
        val sampleRate = sampleRateObject ?: 16000
        if (sampleRate <= 8000) {
            binding.rate8kLayout.chip.isChecked = true
        } else if (sampleRate <= 16000) {
            binding.rate16kLayout.chip.isChecked = true
        } else if (sampleRate <= 22050) {
            binding.rate22kLayout.chip.isChecked = true
        } else if (sampleRate <= 32000) {
            binding.rate32kLayout.chip.isChecked = true
        } else if (sampleRate <= 44100) {
            binding.rate44kLayout.chip.isChecked = true
        }

        var secondsObject: Int? = null
        try {
            secondsObject = separated[2].toInt()
        } catch (_: NumberFormatException) {
        }
        val secondsPerFile = secondsObject ?: 3600
        binding.time12hLayout.chip.isChecked = false
        binding.time4hLayout.chip.isChecked = false
        binding.time1hLayout.chip.isChecked = false
        binding.time1mLayout.chip.isChecked = false
        binding.time10sLayout.chip.isChecked = false
        if (secondsPerFile <= 10) {
            binding.time10sLayout.chip.isChecked = true
        } else if (secondsPerFile <= 60) {
            binding.time1mLayout.chip.isChecked = true
        } else if (secondsPerFile <= 3600) {
            binding.time1hLayout.chip.isChecked = true
        } else if (secondsPerFile <= 14400) {
            binding.time4hLayout.chip.isChecked = true
        } else if (secondsPerFile <= 43200) {
            binding.time12hLayout.chip.isChecked = true
        }

        location = separated[3].trim()
        binding.elocBtNameEditText.setText(location)
    }

    private fun setMicData() {
        // todo: must be obtained directly from firmware
        val data = preferencesManager.getMicData()
        val separated = data.split("#")
        if (separated.size < 2) {
            return
        }

        binding.micTypeEditText.setText(separated[1].trim())

        var gainObject: Int? = null
        try {
            gainObject = if (separated[2].trim().lowercase().contains("low")) 14 else 0
        } catch (_: NumberFormatException) {
        }
        binding.lowGainLayout.chip.isChecked = false
        binding.highGainLayout.chip.isChecked = false
        when (GainType.fromValue(gainObject)) {
            GainType.Low -> binding.lowGainLayout.chip.isChecked = true
            GainType.High -> binding.highGainLayout.chip.isChecked = true
        }
    }

    private fun setToolbar() {
        supportActionBar?.title = getString(R.string.eloc_device_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setListeners() {
        binding.sampleRateChipGroup.setOnCheckedStateChangeListener { _, _ -> setChipColors() }
        binding.fileTimeChipGroup.setOnCheckedStateChangeListener { _, _ -> setChipColors() }
        binding.micGainChipGroup.setOnCheckedStateChangeListener { _, _ -> setChipColors() }
        binding.btStateChipGroup.setOnCheckedStateChangeListener { _, _ -> setChipColors() }
        binding.commandLineButton.setOnClickListener { runCommandLine() }
        binding.recordingButton.setOnClickListener { runRecordingCommand() }
        binding.microphoneTypeButton.setOnClickListener { runMicTypeCommand() }
        binding.microphoneGainButton.setOnClickListener { runMicGainCommand() }
        binding.fileHeaderButton.setOnClickListener { runFileHeaderCommand() }
        binding.hideAdvancedOptionsButton.setOnClickListener { hideAdvancedOperations() }
        binding.showAdvancedOptionsButton.setOnClickListener { showAdvancedOperations() }
        binding.instructionsButton.setOnClickListener { ActivityHelper.showInstructions(this) }
        binding.updateFirmwareButton.setOnClickListener { confirmFirmwareUpdate() }
        binding.bluetoothRecordingStateButton.setOnClickListener { saveBtRecordingState() }
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

    private fun getSampleRate(): Int {
        return if (binding.rate44kLayout.chip.isChecked) {
            44100
        } else if (binding.rate32kLayout.chip.isChecked) {
            32000
        } else if (binding.rate22kLayout.chip.isChecked) {
            22050
        } else if (binding.rate16kLayout.chip.isChecked) {
            16000
        } else {
            8000
        }
    }

    private fun getSecondsPerFile(): Int {
        return if (binding.time12hLayout.chip.isChecked) {
            43200
        } else if (binding.time4hLayout.chip.isChecked) {
            14400
        } else if (binding.time1hLayout.chip.isChecked) {
            3600
        } else if (binding.time1mLayout.chip.isChecked) {
            60
        } else if (binding.time10sLayout.chip.isChecked) {
            10
        } else {
            60
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
            val secondsPerFile = getSecondsPerFile()
            val sampleRate = getSampleRate()
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
        val gain = if (binding.lowGainLayout.chip.isChecked) {
            GainType.Low
        } else {
            GainType.High
        }
        val command = getString(R.string.set_mic_gain_template, gain.value)
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

    private fun saveBtRecordingState() {
        if (!paused) {
            val intent = Intent()
            val btOnWhenRecording = binding.btOnLayout.chip.isChecked
            preferencesManager.setBluetoothRecordingState(btOnWhenRecording)
            val command = if (btOnWhenRecording) {
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