package de.eloc.eloc_control_panel.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.chip.Chip
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.ElocData
import de.eloc.eloc_control_panel.data.GainType
import de.eloc.eloc_control_panel.data.SampleRate
import de.eloc.eloc_control_panel.data.TimePerFile
import de.eloc.eloc_control_panel.data.helpers.JsonHelper
import de.eloc.eloc_control_panel.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.databinding.ActivityDeviceSettingsBinding
import de.eloc.eloc_control_panel.interfaces.BluetoothDeviceListener
import org.json.JSONObject

class DeviceSettingsActivity : ThemableActivity() {
    private lateinit var binding: ActivityDeviceSettingsBinding

    // todo: must be obtained directly from firmware
    private val preferencesManager = PreferencesHelper.instance
    private var paused = true
    private var deviceName = ""
    private var location = ""
    private lateinit var sampleRateChips: List<Chip>
    private lateinit var timePerFileChips: List<Chip>
    private lateinit var gainChips: List<Chip>

    private val commandListener = object : BluetoothDeviceListener() {
        override fun onRead(data: ByteArray) {
            try {
                val json = String(data)
                runOnUiThread { parseResponse(json) }
            } catch (_: Exception) {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setChips()
        setDeviceName()
        setoldData()
        setMicData()
        setBtState()

        setData()

        // Set the listeners after setting the data
        setListeners()
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

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.removeCommandListener(commandListener)
    }

    private fun setData() {
        updateSampleRate()
        updateTimePerFile()
        updateMicrophoneGain()
        binding.logSwitch.isChecked = ElocData.logToSDCardEnabled
        updateChipColors()
        showContent()
    }

    private fun parseResponse(json: String) {
        try {
            val root = JSONObject(json)
            val command = JsonHelper.getJSONStringAttribute(DeviceDriver.KEY_CMD, root).lowercase()
            if (
                (command == DeviceDriver.CMD_SET_STATUS.lowercase()) ||
                (command == DeviceDriver.CMD_SET_CONFIG.lowercase())
            ) {
                val message = try {
                    val errorCode =
                        JsonHelper.getJSONNumberAttribute(DeviceDriver.KEY_ECODE, root, -1.0)
                            .toInt()
                    val messageResId = if (errorCode == 0)
                        R.string.new_value_was_applied
                    else
                        R.string.something_went_wrong
                    getString(messageResId)
                } catch (e: Exception) {
                    e.localizedMessage ?: getString(R.string.something_went_wrong)
                }
                binding.coordinator.showSnack(message)
                binding.progressTextView.text = getString(R.string.updating_values)
                DeviceDriver.getDeviceInfo()
            } else if (
                (command == DeviceDriver.CMD_GET_STATUS.lowercase()) ||
                (command == DeviceDriver.CMD_GET_CONFIG.lowercase())
            ) {
                setData()
            }
        } catch (_: Exception) {

        }
    }

    private fun setListeners() {

        DeviceDriver.addCommandListener(commandListener)
        binding.sampleRateChipGroup.setOnCheckedStateChangeListener { _, _ -> updateChipColors() }
        binding.fileTimeChipGroup.setOnCheckedStateChangeListener { _, _ -> updateChipColors() }
        binding.gainChipGroup.setOnCheckedStateChangeListener { _, _ -> updateChipColors() }
        binding.btStateChipGroup.setOnCheckedStateChangeListener { _, _ -> updateChipColors() }
        binding.commandLineButton.setOnClickListener { runCommandLine() }
        binding.recordingButton.setOnClickListener { runRecordingCommand() }
        binding.microphoneTypeButton.setOnClickListener { runMicTypeCommand() }
        binding.fileHeaderButton.setOnClickListener { runFileHeaderCommand() }
        binding.hideAdvancedOptionsButton.setOnClickListener { }
        binding.showAdvancedOptionsButton.setOnClickListener { }
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.updateFirmwareButton.setOnClickListener { confirmFirmwareUpdate() }
        binding.bluetoothRecordingStateButton.setOnClickListener { saveBtRecordingState() }
        binding.toolbar.setNavigationOnClickListener { goBack() }

        sampleRateChips.map { element ->
            element.setOnClickListener {
                if (it is Chip) {
                    if (it.isChecked) {
                        changeSampleRate(it)
                    }
                }
            }
        }

        timePerFileChips.map { element ->
            element.setOnClickListener {
                if (it is Chip) {
                    if (it.isChecked) {
                        changeTimePerFile(it)
                    }
                }
            }
        }

        gainChips.map { element ->
            element.setOnClickListener {
                if (it is Chip) {
                    if (it.isChecked) {
                        changeMicrophoneGain(it)
                    }
                }
            }
        }

        binding.logSwitch.setOnClickListener {
            showProgress()
            val enableLogs = binding.logSwitch.isChecked
            DeviceDriver.setSDCardLogs(enableLogs)
        }
    }

    private fun changeMicrophoneGain(chip: Chip) {
        val desiredGain = when (chip) {
            binding.gainLowLayout.chip -> GainType.Low
            binding.gainHighLayout.chip -> GainType.High
            else -> GainType.Unknown
        }
        if (desiredGain != GainType.Unknown) {
            showProgress()
            DeviceDriver.setMicrophoneGain(desiredGain)
        }
    }

    private fun changeSampleRate(chip: Chip) {
        val desiredRate = when (chip) {
            binding.rate8kLayout.chip -> SampleRate.Rate8k
            binding.rate16kLayout.chip -> SampleRate.Rate16k
            binding.rate22kLayout.chip -> SampleRate.Rate22k
            binding.rate32kLayout.chip -> SampleRate.Rate32k
            binding.rate44kLayout.chip -> SampleRate.Rate44k
            else -> SampleRate.Unknown
        }
        if (desiredRate != SampleRate.Unknown) {
            showProgress()
            DeviceDriver.setSampleRate(desiredRate)
        }
    }

    private fun changeTimePerFile(chip: Chip) {
        val desiredTime = when (chip) {
            binding.time10sLayout.chip -> TimePerFile.Time10s
            binding.time1mLayout.chip -> TimePerFile.Time1m
            binding.time1hLayout.chip -> TimePerFile.Time1h
            binding.time4hLayout.chip -> TimePerFile.Time4h
            binding.time12hLayout.chip -> TimePerFile.Time12h
            else -> TimePerFile.Unknown
        }
        if (desiredTime != TimePerFile.Unknown) {
            showProgress()
            DeviceDriver.setTimePerFile(desiredTime)
        }
    }

    private fun updateChipColors() {
        setChipColors(binding.btOnLayout)
        setChipColors(binding.btOffLayout)

        setChipColors(binding.gainLowLayout)
        setChipColors(binding.gainHighLayout)

        setChipColors(binding.time12hLayout)
        setChipColors(binding.time4hLayout)
        setChipColors(binding.time1hLayout)
        setChipColors(binding.time1mLayout)
        setChipColors(binding.time10sLayout)

        setChipColors(binding.rate44kLayout)
        setChipColors(binding.rate32kLayout)
        setChipColors(binding.rate22kLayout)
        setChipColors(binding.rate16kLayout)
        setChipColors(binding.rate8kLayout)
    }

    private fun setChips() {
        sampleRateChips = listOf(
            binding.rate8kLayout.chip,
            binding.rate16kLayout.chip,
            binding.rate22kLayout.chip,
            binding.rate32kLayout.chip,
            binding.rate44kLayout.chip
        )

        timePerFileChips = listOf(
            binding.time12hLayout.chip,
            binding.time4hLayout.chip,
            binding.time1hLayout.chip,
            binding.time1mLayout.chip,
            binding.time10sLayout.chip
        )

        gainChips = listOf(
            binding.gainLowLayout.chip,
            binding.gainHighLayout.chip
        )

        binding.rate44kLayout.chip.setText(R.string._44k)
        binding.rate32kLayout.chip.setText(R.string._32k)
        binding.rate22kLayout.chip.setText(R.string._22k)
        binding.rate16kLayout.chip.setText(R.string._16k)
        binding.rate8kLayout.chip.setText(R.string._8k)

        binding.time12hLayout.chip.setText(R.string._12h)
        binding.time4hLayout.chip.setText(R.string._4h)
        binding.time1hLayout.chip.setText(R.string._1h)
        binding.time1mLayout.chip.setText(R.string._1m)
        binding.time10sLayout.chip.setText(R.string._10s)

        binding.gainLowLayout.chip.setText(R.string.low)
        binding.gainHighLayout.chip.setText(R.string.high)

        binding.btOnLayout.chip.setText(R.string.on)
        binding.btOffLayout.chip.setText(R.string.off)

    }

    private fun updateMicrophoneGain() {
        binding.gainLowLayout.chip.isChecked = false
        binding.gainHighLayout.chip.isChecked = false
        when (ElocData.microphoneGain) {
            GainType.Low -> binding.gainLowLayout.chip.isChecked = true
            GainType.High -> binding.gainHighLayout.chip.isChecked = true
            else -> {}
        }
    }

    private fun updateSampleRate() {
        binding.rate44kLayout.chip.isChecked = false
        binding.rate32kLayout.chip.isChecked = false
        binding.rate22kLayout.chip.isChecked = false
        binding.rate16kLayout.chip.isChecked = false
        binding.rate8kLayout.chip.isChecked = false
        when (SampleRate.parse(ElocData.sampleRate.toInt())) {
            SampleRate.Rate8k -> binding.rate8kLayout.chip.isChecked = true
            SampleRate.Rate16k -> binding.rate16kLayout.chip.isChecked = true
            SampleRate.Rate22k -> binding.rate22kLayout.chip.isChecked = true
            SampleRate.Rate32k -> binding.rate32kLayout.chip.isChecked = true
            SampleRate.Rate44k -> binding.rate44kLayout.chip.isChecked = true
            else -> {}
        }
    }

    private fun updateTimePerFile() {
        binding.time12hLayout.chip.isChecked = false
        binding.time4hLayout.chip.isChecked = false
        binding.time1hLayout.chip.isChecked = false
        binding.time1mLayout.chip.isChecked = false
        binding.time10sLayout.chip.isChecked = false
        when (TimePerFile.parse(ElocData.secondsPerFile.toInt())) {
            TimePerFile.Time10s -> binding.time10sLayout.chip.isChecked = true
            TimePerFile.Time1m -> binding.time1mLayout.chip.isChecked = true
            TimePerFile.Time1h -> binding.time1hLayout.chip.isChecked = true
            TimePerFile.Time4h -> binding.time4hLayout.chip.isChecked = true
            TimePerFile.Time12h -> binding.time12hLayout.chip.isChecked = true
            else -> {}
        }
    }

    private fun showProgress() {
        binding.progressTextView.text = getString(R.string.applying_changes)
        binding.progressLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }


    // OLD ------------------------------------------------


    companion object {
        const val COMMAND = "command"
        private const val SEPARATOR = "#"
        private const val COMMAND_PREFIX = SEPARATOR + "settings" + SEPARATOR
    }


    private fun setDeviceName() {
        val extras = intent.extras
        deviceName = extras?.getString(DeviceActivity.EXTRA_DEVICE_NAME, "") ?: ""
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
        updateChipColors()
    }


    private fun setoldData() {
        // todo: remove method
        val data = preferencesManager.getDeviceSettings()
        val separated = data.split(SEPARATOR)
        if (separated.size < 4) {
            return
        }
        location = separated[3].trim()
        binding.elocBtNameEditText.setText(location)
    }

    private fun setMicData() {
// todo remove
        val data = "preferencesManager.getMicData()"
        val separated = data.split("#")
        if (separated.size < 2) {
            return
        }

        binding.micTypeEditText.setText(separated[1].trim())



    }



    private fun runCommand(command: String) {
        hideKeyboard()
        val intent = Intent()
        intent.putExtra(COMMAND, command)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun runCommandLine() {
        val command = binding.customCommandEditText.text?.toString()?.trim() ?: ""
        if (command.isEmpty()) {
            showModalAlert(
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
            showModalAlert(
                getString(R.string.required),
                getString(R.string.file_header_missing)
            )
        } else if (!location.matches(regex)) {
            showModalAlert(
                getString(R.string.invalid),
                getString(R.string.invalid_file_header_name)
            )
        } else {
            val secondsPerFile = "getSecondsPerFile()"
            val sampleRate = "getSampleRate()"
            val command =
                COMMAND_PREFIX + sampleRate + SEPARATOR + secondsPerFile + SEPARATOR + location
            runCommand(command)
        }
    }

    private fun runMicTypeCommand() {
        val type = binding.micTypeEditText.text.toString().trim()
        if (type.isEmpty()) {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.mic_type_missing)
            )
        } else {
            val command = getString(R.string.set_mic_type_template, type)
            runCommand(command)
        }
    }

    private fun runFileHeaderCommand() {
        val name = binding.deviceNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            showModalAlert(
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
        showModalOptionAlert(
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