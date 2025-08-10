package de.eloc.eloc_control_panel.activities.themable

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.children
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.OptionEditorActivity
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.TextEditorActivity
import de.eloc.eloc_control_panel.data.Channel
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.MicrophoneVolumePower
import de.eloc.eloc_control_panel.data.SampleRate
import de.eloc.eloc_control_panel.data.TimePerFile
import de.eloc.eloc_control_panel.databinding.ActivityDeviceSettingsBinding
import de.eloc.eloc_control_panel.driver.Battery
import de.eloc.eloc_control_panel.driver.BtConfig
import de.eloc.eloc_control_panel.driver.Cpu
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.General
import de.eloc.eloc_control_panel.driver.Intruder
import de.eloc.eloc_control_panel.driver.Logs
import de.eloc.eloc_control_panel.driver.Microphone
import de.eloc.eloc_control_panel.activities.formatNumber
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.RangeEditorActivity
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.driver.LoraWan

class DeviceSettingsActivity : ThemableActivity() {
    companion object {
        const val EXTRA_SHOW_RECORDER = "show_recorder"
    }

    private lateinit var binding: ActivityDeviceSettingsBinding
    private val listenerId = "deviceSettingsActivity"

    private var paused = true
    private var statusUpdated = false
    private var configUpdated = false
    private var commandId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        val showMicrophoneSection = extras?.getBoolean(EXTRA_SHOW_RECORDER, false) ?: false

        setMicrophoneSectionState(showMicrophoneSection)
        setIntruder2SectionState(false)
        setLorawanSectionState(false)
        setBluetoothSectionState(false)
        setLogsSectionState(false)
        setCpuSectionState(false)
        setGeneralSectionState(false)
        setAdvancedSectionState(false)
        setBatterySectionState(false)
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
        setData()
        paused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.cancelCommand(commandId)
        DeviceDriver.removeConnectionChangedListener(listenerId)
    }

    private fun setData() {
        binding.generalNodeNameItem.valueText = DeviceDriver.general.nodeName
        binding.generalFileHeaderItem.valueText = DeviceDriver.general.fileHeader
        binding.generalTimePerFileItem.valueText = DeviceDriver.general.timePerFile.toString()

        binding.microphoneTypeItem.valueText = DeviceDriver.microphone.type
        binding.microphoneVolumePowerItem.valueText = DeviceDriver.microphone.volumePower.percentage
        binding.microphoneChannelItem.valueText = DeviceDriver.microphone.channel.value
        binding.microphoneSampleRateItem.valueText = DeviceDriver.microphone.sampleRate.toString()
        binding.microphoneUseApllItem.setSwitch(DeviceDriver.microphone.useAPLL)

        binding.intruder2EnableItem.setSwitch(DeviceDriver.intruder.enabled)
        binding.intruder2ThresholdItem.valueText = DeviceDriver.intruder.threshold.toString()
        binding.intruder2WindowsMsItem.valueText = DeviceDriver.intruder.windowsMs.toString()

        val secs = " secs"
        binding.lorawanEnableItem.setSwitch(DeviceDriver.lorawan.enabled)
        binding.lorawanUplinkIntervalItem.valueText =
            formatNumber(DeviceDriver.lorawan.uplinkIntervalSeconds, secs, 0)
        binding.lorawanRegionItem.valueText = DeviceDriver.lorawan.region


        binding.btEnableAtStartItem.setSwitch(DeviceDriver.bluetooth.enableAtStart)
        binding.btEnableOnTappingItem.setSwitch(DeviceDriver.bluetooth.enableOnTapping)
        binding.btEnableDuringRecordingItem.setSwitch(DeviceDriver.bluetooth.enableDuringRecord)
        binding.btOffTimeoutSecondsItem.valueText =
            formatNumber(DeviceDriver.bluetooth.offTimeoutSeconds, secs, 0)

        val mHz = " MHz"
        binding.cpuEnableLightSleepItem.setSwitch(DeviceDriver.cpu.enableLightSleep)
        binding.cpuMinFrequencyItem.valueText = formatNumber(DeviceDriver.cpu.minFrequencyMHz, mHz)
        binding.cpuMaxFrequencyItem.valueText = formatNumber(DeviceDriver.cpu.maxFrequencyMHz, mHz)

        binding.logsLogToSdCardItem.setSwitch(DeviceDriver.logs.logToSdCard)
        binding.logsFilenameItem.valueText = DeviceDriver.logs.filename
        binding.logsMaxFilesItem.valueText = DeviceDriver.logs.maxFiles.toString()
        binding.logsMaxFileSizeItem.valueText = DeviceDriver.logs.maxFileSize.toString()

        binding.batteryNoBatModeItem.setSwitch(DeviceDriver.battery.noBatteryMode)
        binding.batteryAverageSamplesItem.valueText = DeviceDriver.battery.avgSamples.toString()
        binding.batteryAverageIntervalItem.valueText =
            formatNumber(DeviceDriver.battery.avgIntervalSecs, secs, 0)
        binding.batteryUpdateIntervalItem.valueText =
            formatNumber(DeviceDriver.battery.updateIntervalSecs, secs, 0)

        showContent()
    }

    private fun setListeners() {
        DeviceDriver.addConnectionChangedListener(listenerId, ::onConnectionChanged)
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.toolbar.setNavigationOnClickListener { goBack() }

        setBatteryListeners()
        setGeneralListeners()
        setCpuListeners()
        setLogsListeners()
        setIntruder2Listeners()
        setLorawanListeners()
        setBtListeners()
        setMicrophoneListeners()
        setAdvancedListeners()
    }

    private fun onConnectionChanged(status: ConnectionStatus) {
        if (status == ConnectionStatus.Inactive) {
            goBack()
        }
    }

    private fun setAdvancedListeners() {
        binding.advancedSectionTextView.setOnClickListener {
            setAdvancedSectionState(binding.advancedCommandLineItem.visibility != View.VISIBLE)
        }
        binding.advancedCommandLineItem.setOnClickListener {
            val intent = Intent(this, CommandLineActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setGeneralListeners() {
        binding.generalSectionTextView.setOnClickListener {
            setGeneralSectionState(binding.generalNodeNameItem.visibility != View.VISIBLE)
        }
        binding.generalNodeNameItem.setOnClickListener {
            openTextEditor(
                General.NODE_NAME,
                getString(R.string.node_device_name),
                DeviceDriver.general.nodeName,
            )
        }
        binding.generalFileHeaderItem.setOnClickListener {
            openTextEditor(
                General.FILE_HEADER,
                getString(R.string.file_header),
                DeviceDriver.general.fileHeader,
            )
        }
        binding.generalTimePerFileItem.setOnClickListener {
            val options = listOf(
                TimePerFile.Time10s,
                TimePerFile.Time1m,
                TimePerFile.Time1h,
                TimePerFile.Time4h,
                TimePerFile.Time12h,
            ).map {
                "${it.seconds}|$it"
            }
            OptionEditorActivity.open(
                this,
                General.SECONDS_PER_FILE,
                getString(R.string.time_per_file),
                DeviceDriver.general.timePerFile.toString(),
                options
            )
        }
    }

    private fun setLogsListeners() {
        binding.logsSectionTextView.setOnClickListener {
            setLogsSectionState(binding.logsLogToSdCardItem.visibility != View.VISIBLE)
        }
        binding.logsLogToSdCardItem.setSwitchClickedListener {
            val checked = binding.logsLogToSdCardItem.isChecked
            Command.createSetConfigPropertyCommand(
                Logs.LOG_TO_SD_CARD,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                }
            ) { refresh() }
        }
        binding.logsFilenameItem.setOnClickListener {
            var filename = DeviceDriver.logs.filename
            val splitter = "/"
            var prefix = ""
            if (filename.contains(splitter)) {
                val parts = filename.split(splitter).toMutableList()
                filename = parts.removeAt(parts.size - 1)
                prefix = parts.joinToString(splitter) + splitter
            }
            openTextEditor(
                Logs.FILENAME,
                getString(R.string.logs_filename),
                filename,
                prefix = prefix
            )
        }
        binding.logsMaxFilesItem.setOnClickListener {
            openTextEditor(
                Logs.MAX_FILES,
                getString(R.string.logs_max_files),
                DeviceDriver.logs.maxFiles.toString(),
                true,
            )
        }
        binding.logsMaxFileSizeItem.setOnClickListener {
            openTextEditor(
                Logs.MAX_FILE_SIZE,
                getString(R.string.logs_max_file_size),
                DeviceDriver.logs.maxFileSize.toString(),
                true,
            )
        }
    }

    private fun setBatteryListeners() {
        binding.batterySectionTextView.setOnClickListener {
            setBatterySectionState(binding.batteryAverageSamplesItem.visibility != View.VISIBLE)
        }
        binding.batteryNoBatModeItem.setSwitchClickedListener {
            val checked = binding.batteryNoBatModeItem.isChecked
            Command.createSetConfigPropertyCommand(
                Battery.NO_BATTERY_MODE,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                }) { refresh() }
        }
        binding.batteryAverageSamplesItem.setOnClickListener {
            openTextEditor(
                Battery.AVERAGE_SAMPLES,
                getString(R.string.battery_average_samples),
                DeviceDriver.battery.avgSamples.toString(),
                isNumeric = true,
                minimum = 1.0,
            )
        }
        binding.batteryAverageIntervalItem.setOnClickListener {
            openTextEditor(
                Battery.AVERAGE_INTERVAL,
                getString(R.string.battery_average_interval),
                DeviceDriver.battery.avgIntervalSecs.toInt().toString(),
                isNumeric = true,
                minimum = 0.0,
            )
        }
        binding.batteryUpdateIntervalItem.setOnClickListener {
            openTextEditor(
                Battery.UPDATE_INTERVAL,
                getString(R.string.battery_update_interval),
                DeviceDriver.battery.updateIntervalSecs.toInt().toString(),
                isNumeric = true,
                minimum = 5.0,
            )
        }
    }

    private fun setCpuListeners() {
        binding.cpuSectionTextView.setOnClickListener {
            setCpuSectionState(binding.cpuEnableLightSleepItem.visibility != View.VISIBLE)
        }
        binding.cpuEnableLightSleepItem.setSwitchClickedListener {
            val checked = binding.cpuEnableLightSleepItem.isChecked
            Command.createSetConfigPropertyCommand(
                Cpu.ENABLE_LIGHT_SLEEP,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                }) { refresh() }
        }
        binding.cpuMinFrequencyItem.setOnClickListener {
            openTextEditor(
                Cpu.MIN_FREQUENCY,
                getString(R.string.minimum_cpu_frequency),
                DeviceDriver.cpu.minFrequencyMHz.toString(),
                isNumeric = true,
                minimum = 1.0,
            )
        }
        binding.cpuMaxFrequencyItem.setOnClickListener {
            openTextEditor(
                Cpu.MAX_FREQUENCY,
                getString(R.string.maximum_cpu_frequency),
                DeviceDriver.cpu.maxFrequencyMHz.toString(),
                isNumeric = true,
                minimum = 80.0,
            )
        }
    }

    private fun setLorawanListeners() {
        binding.lorawanSectionTextView.setOnClickListener {
            setLorawanSectionState(binding.lorawanEnableItem.visibility != View.VISIBLE)
        }
        binding.lorawanEnableItem.setSwitchClickedListener {
            val checked = binding.lorawanEnableItem.isChecked
            Command.createSetConfigPropertyCommand(
                LoraWan.ENABLED,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                },
            ) { refresh() }
        }
        binding.lorawanRegionItem.setOnClickListener {
            openTextEditor(
                LoraWan.REGION,
                getString(R.string.region),
                DeviceDriver.lorawan.region,
            )
        }
        binding.lorawanUplinkIntervalItem.setOnClickListener {
            val prettyCurrentInterval =
                LoraWan.prettifyTime(DeviceDriver.lorawan.uplinkIntervalSeconds)
            val currentInterval = DeviceDriver.lorawan.uplinkIntervalSeconds
            RangeEditorActivity.openRangeEditor(
                this,
                LoraWan.UPLINK_INTERVAL,
                getString(R.string.uplink_interval),
                "$currentInterval ($prettyCurrentInterval)",
                DeviceDriver.lorawan.uplinkIntervalSeconds.toFloat(),
                LoraWan.MIN_INTERVAL_SECS.toFloat(),
                LoraWan.MAX_INTERVAL_SECS.toFloat()
            )
        }
    }

    private fun setIntruder2Listeners() {
        binding.intruder2SectionTextView.setOnClickListener {
            setIntruder2SectionState(binding.intruder2EnableItem.visibility != View.VISIBLE)
        }
        binding.intruder2EnableItem.setSwitchClickedListener {
            val checked = binding.intruder2EnableItem.isChecked
            Command.createSetConfigPropertyCommand(
                Intruder.ENABLED,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                },
            ) { refresh() }
        }
        binding.intruder2ThresholdItem.setOnClickListener {
            openTextEditor(
                Intruder.THRESHOLD,
                getString(R.string.intruder_threshold),
                DeviceDriver.intruder.threshold.toString(),
                true,
            )
        }
        binding.intruder2WindowsMsItem.setOnClickListener {
            openTextEditor(
                Intruder.WINDOWS_MS,
                getString(R.string.intruder_windows_ms),
                DeviceDriver.intruder.windowsMs.toString(),
                true,
            )
        }
    }

    private fun setBtListeners() {
        binding.btSectionTextView.setOnClickListener {
            setBluetoothSectionState(binding.btEnableAtStartItem.visibility != View.VISIBLE)
        }
        binding.btEnableAtStartItem.setSwitchClickedListener {
            val checked = binding.btEnableAtStartItem.isChecked
            Command.createSetConfigPropertyCommand(
                BtConfig.ENABLE_AT_START,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                },
            ) { refresh() }
        }
        binding.btEnableOnTappingItem.setSwitchClickedListener {
            val checked = binding.btEnableOnTappingItem.isChecked
            Command.createSetConfigPropertyCommand(
                BtConfig.ENABLE_ON_TAPPING,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                },
            ) { refresh() }
        }
        binding.btEnableDuringRecordingItem.setSwitchClickedListener {
            val checked = binding.btEnableDuringRecordingItem.isChecked
            Command.createSetConfigPropertyCommand(
                BtConfig.ENABLE_DURING_RECORD,
                checked.toString(),
                ::runCommand,
                {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                },
            ) { refresh() }
        }
        binding.btOffTimeoutSecondsItem.setOnClickListener {
            openTextEditor(
                BtConfig.OFF_TIME_OUT_SECONDS,
                getString(R.string.bluetooth_timeout_off_seconds),
                DeviceDriver.bluetooth.offTimeoutSeconds.toString(),
                true,
                minimum = 10.0,
            )
        }
    }

    private fun setMicrophoneListeners() {
        binding.microphoneSectionTextView.setOnClickListener {
            setMicrophoneSectionState(binding.microphoneTypeItem.visibility != View.VISIBLE)
        }
        binding.microphoneTypeItem.setOnClickListener {
            openTextEditor(
                Microphone.TYPE,
                getString(R.string.microphone_type),
                DeviceDriver.microphone.type,
            )
        }
        binding.microphoneVolumePowerItem.setOnClickListener {
            RangeEditorActivity.openRangeEditor(
                this,
                Microphone.VOLUME_POWER,
                getString(R.string.microphone_gain),
                DeviceDriver.microphone.volumePower.percentage,
                DeviceDriver.microphone.volumePower.rawValue,
                MicrophoneVolumePower.MINIMUM,
                MicrophoneVolumePower.MAXIMUM
            )
        }

        binding.microphoneChannelItem.setOnClickListener {
            val options = listOf(Channel.Left, Channel.Right, Channel.Stereo).map {
                "${it.value}|$it"
            }
            OptionEditorActivity.open(
                this,
                Microphone.CHANNEL,
                getString(R.string.recorder_channel),
                DeviceDriver.microphone.channel.value,
                options
            )
        }

        binding.microphoneSampleRateItem.setOnClickListener {
            val options = listOf(
                SampleRate.Rate8k,
                SampleRate.Rate16k,
                SampleRate.Rate22k,
                SampleRate.Rate32k,
                SampleRate.Rate44k,
            ).map {
                "${it.code}|$it"
            }
            OptionEditorActivity.open(
                this,
                Microphone.SAMPLE_RATE,
                getString(R.string.recorder_sample_rate),
                DeviceDriver.microphone.sampleRate.code.toString(),
                options
            )
        }

        binding.microphoneUseApllItem.setSwitchClickedListener {
            val checked = binding.microphoneUseApllItem.isChecked
            Command.createSetConfigPropertyCommand(
                property = Microphone.USE_APLL,
                value = checked.toString(),
                commandCreatedCallback = ::runCommand,
                errorCallback = {
                    showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                },
            ) { refresh() }
        }
    }

    private fun runCommand(command: Command) {
        commandId = command.id
        showProgress()
        DeviceDriver.processCommandQueue(command)
    }

    private fun openTextEditor(
        property: String,
        settingName: String,
        currentValue: String,
        isNumeric: Boolean = false,
        minimum: Double? = null,
        prefix: String = ""
    ) {
        val intent = Intent(this, TextEditorActivity::class.java)
        intent.putExtra(BaseEditorActivity.EXTRA_SETTING_NAME, settingName)
        intent.putExtra(BaseEditorActivity.EXTRA_CURRENT_VALUE, currentValue)
        intent.putExtra(BaseEditorActivity.EXTRA_PROPERTY, property)
        intent.putExtra(BaseEditorActivity.EXTRA_IS_NUMERIC, isNumeric)
        intent.putExtra(BaseEditorActivity.EXTRA_PREFIX, prefix)
        if (minimum != null) {
            intent.putExtra(BaseEditorActivity.EXTRA_MINIMUM, minimum)
        }
        startActivity(intent)
    }

    private fun setLorawanSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.lorawanSection.children.forEach { child ->
            if (child == binding.lorawanSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.lorawanSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setIntruder2SectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.intruder2Section.children.forEach { child ->
            if (child == binding.intruder2SectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.intruder2SectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setBluetoothSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.btSection.children.forEach { child ->
            if (child == binding.btSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.btSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setLogsSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.logSection.children.forEach { child ->
            if (child == binding.logsSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.logsSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setAdvancedSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.advancedSection.children.forEach { child ->
            if (child == binding.advancedSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.advancedSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setGeneralSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.generalSection.children.forEach { child ->
            if (child == binding.generalSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.generalSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setBatterySectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.batterySection.children.forEach { child ->
            if (child == binding.batterySectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.batterySectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setCpuSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.cpuSection.children.forEach { child ->
            if (child == binding.cpuSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.cpuSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun setMicrophoneSectionState(expanded: Boolean) {
        val state = if (expanded) View.VISIBLE else View.GONE
        binding.microphoneSection.children.forEach { child ->
            if (child == binding.microphoneSectionTextView) {
                return@forEach
            }
            child.visibility = state
        }

        val icon = if (expanded) {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_up)
        } else {
            ContextCompat.getDrawable(this, R.drawable.keyboard_arrow_down)
        }
        binding.microphoneSectionTextView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            icon,
            null
        )
    }

    private fun showProgress() {
        binding.progressIndicator.text = getString(R.string.applying_changes)
        binding.progressIndicator.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressIndicator.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun refresh() {
        runOnUiThread {
            statusUpdated = false
            configUpdated = false
            binding.progressIndicator.text = getString(R.string.updating_values)
            DeviceDriver.getElocInformation {
                runOnUiThread {
                    val commandType = DeviceDriver.getCommandType(it)
                    if (!statusUpdated && (commandType == CommandType.GetStatus)) {
                        statusUpdated = true
                    }
                    if (!configUpdated && (commandType == CommandType.GetConfig)) {
                        configUpdated = true
                    }
                    if (statusUpdated && configUpdated) {
                        setData()
                    }
                }
            }
        }
    }
}