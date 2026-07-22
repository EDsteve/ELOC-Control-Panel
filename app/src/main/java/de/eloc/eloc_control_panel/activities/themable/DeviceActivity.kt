package de.eloc.eloc_control_panel.activities.themable

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.formatNumber
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.onWriteCommandError
import de.eloc.eloc_control_panel.activities.prettifyTime
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.RangeEditorActivity
import de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.TextEditorActivity
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.GpsData
import de.eloc.eloc_control_panel.data.GpsDataSource
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.databinding.LayoutModeChooserBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.DutyCycle
import de.eloc.eloc_control_panel.driver.Intruder
import de.eloc.eloc_control_panel.driver.LoraSignalStrength
import de.eloc.eloc_control_panel.driver.LoraWan
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback
import de.eloc.eloc_control_panel.receivers.ElocReceiver
import kotlin.math.roundToInt

// todo: add refresh menu item for old API levels

class DeviceActivity : ThemableActivity() {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        private const val DISABLED_SECTION_ALPHA = 0.4f

        // How often the status page silently re-reads status from the device
        // while it is foregrounded. Long enough that the extra BT/SD/CPU load on
        // the device during AI recording is negligible; short enough that
        // battery, SD fill, GPS fix and the recording timers feel live.
        private const val AUTO_REFRESH_INTERVAL_MS = 15_000L
    }

    private enum class ViewMode {
        ScanningView,
        ConnectingView,
        LocationPendingView,
        SyncingTimeView,
        ContentView,
        Disconnected,
        FetchingDataView,
        SettingRecordMode,
        StoppingRecordMode,
    }

    private val listenerId = "deviceActivity"
    private var hasSDCardError = false
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var timeSyncHandled = false
    private var showDeviceInfoStarted = false
    private var refreshing = false
    private var paused = false
    private var statusReceived = false
    private var configReceived = false
    private var gpsLocationUpdate: GpsData? = null
    private lateinit var elocReceiver: ElocReceiver

    // Periodic silent status refresh: keeps the on-screen numbers ticking over
    // without a manual pull-to-refresh. Runs only while the page is foregrounded
    // (started in onResume, stopped in onPause) and each tick is gated so it
    // never competes with a user action or a firmware transfer.
    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            maybeAutoRefreshStatus()
            autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL_MS)
        }
    }

    private val getCommandCompletedListener = GetCommandCompletedCallback {
        runOnUiThread {
            // Hide the progress indicator when data is received i.e., there is a connection
            binding.swipeRefreshLayout.isRefreshing = false
            checkReceivedInfoType(it)
        }
    }

    private val setCommandCompletedCallback = SetCommandCompletedCallback { success, type ->
        runOnUiThread {
            when (type) {
                CommandType.SetRecordMode -> {
                    displayRecordingState()
                    if (success) {
                        // After starting recording mode, upload full info (status + config)
                        // with new session ID and location to database (best of phone / ELOC fix)
                        DeviceDriver.getElocInformation(effectiveGpsData(), true) {
                            val commandType = DeviceDriver.getCommandType(it)
                            checkReceivedInfoType(commandType)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
    }

    override fun onResume() {
        super.onResume()
        paused = false
        gpsLocationUpdate = null
        LocationHelper.startUpdates {
            runOnUiThread {
                gpsLocationUpdate = it
                updateGpsViews()
            }
        }
        // Coming back from a settings editor: re-fetch so edited values show up.
        if (showDeviceInfoStarted && statusReceived && configReceived) {
            refreshDeviceInfo()
        }
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        paused = true
        stopAutoRefresh()
        LocationHelper.stopUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
        gpsLocationUpdate = null
        DeviceDriver.removeConnectionChangedListener(listenerId)
        DeviceDriver.removeWriteCommandLister(listenerId)
        DeviceDriver.removeOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.removeOnGetCommandCompletedListener(getCommandCompletedListener)
        DeviceDriver.disconnect()
    }

    private fun onConnectionStatusChanged(status: ConnectionStatus) {
        runOnUiThread {
            refreshing = false
            binding.swipeRefreshLayout.isRefreshing = false
            binding.toolbar.menu.clear()
            // Any state change re-enables the manual sync button (it is
            // disabled while a manual setTime round-trip is in flight).
            setSyncTimeButtonBusy(false)
            when (status) {
                ConnectionStatus.Active -> showDeviceInfo()

                ConnectionStatus.Inactive -> {
                    binding.swipeRefreshLayout.isEnabled = true
                    timeSyncHandled = false
                    showDeviceInfoStarted = false
                    setViewMode(ViewMode.Disconnected)
                }

                ConnectionStatus.Pending -> {
                    timeSyncHandled = false
                    showDeviceInfoStarted = false
                    binding.swipeRefreshLayout.isEnabled = false
                    setViewMode(ViewMode.ConnectingView)
                }
            }
        }
    }

    private fun initialize() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.ui_window)
        displayRecordingState()
        registerListeners()
        elocReceiver = ElocReceiver(null) { elocFound(it) }
        onConnectionStatusChanged(ConnectionStatus.Inactive)
        val extras = intent.extras
        deviceAddress = extras?.getString(EXTRA_DEVICE_ADDRESS, "")?.trim() ?: ""
        if (deviceAddress.isEmpty()) {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.device_address_required),
                ::goBack
            )
        } else {
            if (!Preferences.hasValidProfile) {
                showModalAlert(
                    getString(R.string.required),
                    getString(R.string.aranger_name_required),
                    ::goBack
                )
            } else {
                binding.toolbar.title = getString(R.string.getting_node_name)
                binding.toolbar.subtitle = getString(R.string.eloc_user, Preferences.rangerName)
                binding.appVersionText.text = App.versionName
                updateGpsViews()
                setListeners()
                connectToDevice()
            }
        }
    }

    private fun registerListeners() {
        DeviceDriver.addWriteCommandErrorListener(listenerId, ::onWriteCommandError)
        DeviceDriver.addOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.addOnGetCommandCompletedListener(getCommandCompletedListener)
    }

    private fun skipTimeSync() {
        showModalAlert(
            getString(R.string.time_sync),
            getString(R.string.time_sync_rationale),
        ) {
            onTimeSyncCompleted()
        }
    }

    private fun onTimeSyncCompleted() {
        // Guard against double-completion (e.g. the skip dialog is dismissed
        // after the pending setTime response already arrived).
        if (!timeSyncHandled) {
            timeSyncHandled = true
            continueDeviceInfo()
        }
    }

    private fun showDeviceInfo() {
        runOnUiThread {
            if (!showDeviceInfoStarted) {
                showDeviceInfoStarted = true
                setViewMode(ViewMode.FetchingDataView)
                // First getStatus call checks the recording state AND the on-board GPS
                // clock state, so synchronizeTime() can skip the setTime round-trip when
                // GPS already disciplines the device clock. Not saved to database.
                DeviceDriver.getStatus(saveToDatabase = false) {
                    runOnUiThread { synchronizeTime() }
                }
            }
        }
    }

    private fun continueDeviceInfo() {
        runOnUiThread {
            setViewMode(ViewMode.FetchingDataView)
            val isRecording = DeviceDriver.session.recordingState.isActive
            if (isRecording && (gpsLocationUpdate == null)) {
                val savedUpdate = Preferences.lastKnownGpsLocation
                gpsLocationUpdate =
                    savedUpdate ?: GpsData(source = GpsDataSource.Unknown)
            } else if (gpsLocationUpdate == null) {
                gpsLocationUpdate = GpsData(source = GpsDataSource.Unknown)
            }
            // Don't block on a GPS modal — go straight to data fetch.
            // The GPS gauge already conveys whether the fix is real, cached, or unknown.
            onFirstLocationReceived()
        }
    }

    private fun onFirstLocationReceived() {
        setViewMode(ViewMode.FetchingDataView)

        // Check if the device is already in an active recording mode
        val isRecording = DeviceDriver.session.recordingState.isActive

        if (isRecording) {
            // Device is already recording - upload status only to database
            // (no need to update location or config, just get current status)
            DeviceDriver.getStatus {
                runOnUiThread {
                    statusReceived = true
                    // Also get config for UI display (without saving to database)
                    DeviceDriver.getElocInformation(null, false) {
                        val type = DeviceDriver.getCommandType(it)
                        checkReceivedInfoType(type)
                    }
                }
            }
        } else {
            // Device is NOT recording - get info for UI display only, no database upload
            // (saveNextInfoResponse = false means no upload to database)
            DeviceDriver.getElocInformation(effectiveGpsData(), false) {
                val type = DeviceDriver.getCommandType(it)
                checkReceivedInfoType(type)
            }
        }
    }

    private fun checkReceivedInfoType(commandType: CommandType) {
        runOnUiThread {
            if ((!configReceived) && (commandType == CommandType.GetConfig)) {
                configReceived = true
            }
            if ((!statusReceived) && (commandType == CommandType.GetStatus)) {
                statusReceived = true
            }

            if (configReceived && statusReceived) {
                onElocInfoReceived()
            }
        }
    }

    private fun onElocInfoReceived() {
        if (statusReceived && configReceived) {
            // Hide the progress indicator when data is received i.e., there is a connection
            binding.swipeRefreshLayout.isRefreshing = false
            binding.swipeRefreshLayout.isEnabled = true
            binding.toolbar.menu.clear()
            menuInflater.inflate(R.menu.app_bar_device, binding.toolbar.menu)
            setConfigInfo()
            setStatusInfo()
            setViewMode(ViewMode.ContentView)
        }
    }

    private fun startAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
        autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL_MS)
    }

    private fun stopAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
    }

    // One auto-refresh tick: enqueue a silent, status-only read if — and only
    // if — the page is showing live content and nothing else is using the link.
    // The existing getCommandCompletedListener rebinds the status views when the
    // response arrives, so no explicit callback is needed here.
    private fun maybeAutoRefreshStatus() {
        // Only while the status page is actually showing content...
        val showingContent = showDeviceInfoStarted && statusReceived && configReceived
        if (paused || refreshing || !showingContent) {
            return
        }
        // ...and the link is connected and idle (isIdle also excludes an
        // in-progress firmware transfer). Skipping here just defers to the next
        // tick — it never delays a user-initiated command.
        if (!DeviceDriver.isConnected || !DeviceDriver.isIdle) {
            return
        }
        // Status only, no database upload (so this does not spam Firestore).
        DeviceDriver.getStatus(saveToDatabase = false) { }
    }

    // Re-fetch status + config (no database upload) and rebind the UI when both arrive.
    private fun refreshDeviceInfo() {
        runOnUiThread {
            statusReceived = false
            configReceived = false
            binding.swipeRefreshLayout.isRefreshing = true
            DeviceDriver.getElocInformation(null, false) {
                val type = DeviceDriver.getCommandType(it)
                checkReceivedInfoType(type)
            }
        }
    }

    private fun setStatusInfo() {
        binding.sessionIdItem.valueText = DeviceDriver.session.id
        val detectionDuration = if (DeviceDriver.session.detecting) {
            DeviceDriver.session.detectingDurationSeconds.toInt()
        } else {
            -1
        }
        binding.detectionDurationItem.valueText = if ((detectionDuration <= 0)) {
            getString(R.string.not_detecting)
        } else {
            TimeHelper.formatSeconds(this, detectionDuration)
        }
        val recordingDuration = DeviceDriver.session.recordingDurationSeconds.toInt()
        binding.recordingDurationItem.valueText = if (recordingDuration <= 0) {
            getString(R.string.not_recording)
        } else {
            TimeHelper.formatSeconds(this, recordingDuration)
        }
        binding.statEventsValue.text = DeviceDriver.session.eventsDetected.toString()
        binding.aiModelItem.valueText = DeviceDriver.session.aiModel
        binding.statRecBootValue.text =
            TimeHelper.formatHours(this, DeviceDriver.general.recHoursSinceBoot)
        binding.firmwareVersionText.text = DeviceDriver.general.version.ifBlank { "—" }
        // Device-reported time, with a source suffix ("· GPS" / "· Phone") when the firmware reports
        // one (V1.54+); older firmware sends no timeSource, so the bare time is shown (or a dash).
        val baseTime = DeviceDriver.general.deviceTime
        binding.statTimeValue.text = if (baseTime.isBlank()) {
            "—"
        } else {
            when (DeviceDriver.general.deviceTimeSource) {
                "gps" -> getString(
                    R.string.time_with_source, baseTime, getString(R.string.time_source_gps)
                )
                "app" -> getString(
                    R.string.time_with_source, baseTime, getString(R.string.time_source_phone)
                )
                else -> baseTime
            }
        }
        binding.statGpsValue.text = describeDeviceGps()
        binding.statUptimeValue.text =
            TimeHelper.formatHours(this, DeviceDriver.general.uptimeHours)

        updateBatteryGauge()
        updateStorageGauge()
        updateLoraSection()
        updateSchedulerSection()
        updateIntruderSection()
        displayRecordingState()
        // Repaint the accuracy gauge on every status refresh (15 s auto-refresh) so the phone↔ELOC
        // comparison stays current between phone-location callbacks.
        updateGpsViews()
    }

    private fun updateBatteryGauge() {
        val level = DeviceDriver.battery.level
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batterySubLabel.text =
            getString(R.string.voltage_template, DeviceDriver.battery.voltage)
        binding.batteryGauge.updateValue(level)
        binding.batteryVoltItem.valueText = formatNumber(DeviceDriver.battery.voltage, "V")
        binding.batteryTypeItem.valueText = DeviceDriver.battery.type
    }

    private fun updateStorageGauge() {
        val free = DeviceDriver.sdCard.freeGb
        val freePercentage = DeviceDriver.sdCard.freePercentage
        val gb = DeviceDriver.sdCard.sizeGb
        binding.storageGauge.errorMode = (free <= 0) || (gb <= 0.0)
        if (binding.storageGauge.errorMode) {
            binding.storageStatus.text = ""
            binding.storageSubLabel.text = getString(R.string.no_sd_card)
            binding.storageGauge.updateValue(0.0)
        } else {
            binding.storageStatus.text =
                getString(R.string.gauge_battery_level, freePercentage.toInt())
            binding.storageSubLabel.text = getString(R.string.free_space_template, free)
            binding.storageGauge.updateValue(freePercentage)
        }
    }

    /**
     * One-line status of the device's on-board GPS for the status list. Distinct from the phone GPS
     * shown by the gps_gauge.
     */
    private fun describeDeviceGps(): String {
        val gps = DeviceDriver.gps
        return when {
            !gps.present -> getString(R.string.not_available)
            gps.hasFix -> getString(R.string.gps_fix, gps.satellites)
            gps.powered -> getString(R.string.gps_searching, gps.satellites)
            else -> getString(R.string.gps_idle)
        }
    }

    private fun setConfigInfo() {
        binding.toolbar.title = DeviceDriver.general.nodeName
        binding.statSampleRateValue.text = DeviceDriver.microphone.sampleRate.toString()
        binding.microphoneTypeItem.valueText = DeviceDriver.microphone.type
        binding.statGainValue.text = DeviceDriver.microphone.volumePower.percentage
        binding.timePerFileItem.valueText = DeviceDriver.general.timePerFile.toString()
        binding.lastLocationItem.valueText = DeviceDriver.general.lastLocation
        val enabledLabel =
            getString(if (DeviceDriver.bluetooth.enableDuringRecord) R.string.on else R.string.off)
        binding.bluetoothDuringRecordingItem.valueText = enabledLabel
        binding.fileHeaderItem.valueText = DeviceDriver.general.fileHeader
    }

    // ==================== Toggle sections (LoRa / Scheduler / Intruder) ====================

    private fun updateLoraSection() {
        val lorawan = DeviceDriver.lorawan
        applySectionState(
            enabled = lorawan.enabled,
            switch = binding.loraSwitch,
            stripe = binding.loraStripe,
            content = binding.loraContent,
            icon = binding.loraIcon
        )
        binding.loraRegionItem.valueText = lorawan.region.ifBlank { "—" }
        binding.loraUplinkItem.valueText = prettifyTime(lorawan.uplinkIntervalSeconds)
        binding.loraNetworkItem.valueText = getString(
            if (lorawan.joined) R.string.joined else R.string.lora_not_joined
        )

        // Header: when LoRa is active, replace the static description with live signal
        // status and tint the antenna icon by signal strength.
        if (lorawan.enabled) {
            when {
                !lorawan.joined -> {
                    binding.loraSubtitle.text = getString(R.string.lora_not_joined)
                    tintLoraIcon(R.color.ui_text_secondary)
                }

                !lorawan.hasSignalInfo -> {
                    binding.loraSubtitle.text = getString(R.string.lora_no_signal)
                    tintLoraIcon(R.color.ui_text_secondary)
                }

                else -> {
                    val strength = lorawan.signalStrength
                    binding.loraSubtitle.text = getString(
                        R.string.lora_signal_subtitle,
                        getString(signalLabelRes(strength)),
                        lorawan.rssi
                    )
                    tintLoraIcon(signalColorRes(strength))
                }
            }
        } else {
            binding.loraSubtitle.text = getString(R.string.lora_subtitle)
        }

        // Expanded signal row (unchanged behavior).
        if (!lorawan.joined) {
            binding.loraSignalIcon.setImageResource(R.drawable.rssi_0)
            binding.loraSignalValue.text = getString(R.string.lora_not_joined)
        } else if (!lorawan.hasSignalInfo) {
            binding.loraSignalIcon.setImageResource(R.drawable.rssi_1)
            binding.loraSignalValue.text = getString(R.string.lora_no_signal)
        } else {
            binding.loraSignalIcon.setImageResource(lorawan.signalStrength.iconResource)
            binding.loraSignalValue.text = getString(R.string.lora_signal_dbm, lorawan.rssi)
        }
    }

    private fun tintLoraIcon(colorRes: Int) {
        binding.loraIcon.setColorFilter(ContextCompat.getColor(this, colorRes))
    }

    private fun signalLabelRes(strength: LoraSignalStrength): Int = when (strength) {
        LoraSignalStrength.Excellent -> R.string.signal_excellent
        LoraSignalStrength.Good -> R.string.signal_good
        LoraSignalStrength.Fair -> R.string.signal_fair
        LoraSignalStrength.Poor -> R.string.signal_poor
        LoraSignalStrength.VeryPoor -> R.string.signal_very_poor
    }

    private fun signalColorRes(strength: LoraSignalStrength): Int = when (strength) {
        LoraSignalStrength.Excellent -> R.color.ui_green
        LoraSignalStrength.Good -> R.color.ui_green
        LoraSignalStrength.Fair -> R.color.ui_amber
        LoraSignalStrength.Poor -> R.color.ui_orange
        LoraSignalStrength.VeryPoor -> R.color.ui_red
    }

    private fun updateSchedulerSection() {
        val dutyCycle = DeviceDriver.dutyCycle
        applySectionState(
            enabled = dutyCycle.enabled,
            switch = binding.schedulerSwitch,
            stripe = binding.schedulerStripe,
            content = binding.schedulerContent,
            icon = binding.schedulerIcon
        )
        binding.dutyAwakeItem.valueText = prettifyTime(dutyCycle.awakeDurationS)
        binding.dutySleepItem.valueText = prettifyTime(dutyCycle.sleepDurationS)
        // When active, summarize the awake/sleep timing in the header.
        binding.schedulerSubtitle.text = if (dutyCycle.enabled) {
            getString(
                R.string.scheduler_active_subtitle,
                prettifyTime(dutyCycle.awakeDurationS),
                prettifyTime(dutyCycle.sleepDurationS)
            )
        } else {
            getString(R.string.scheduler_subtitle)
        }
    }

    private fun updateIntruderSection() {
        val intruder = DeviceDriver.intruder
        applySectionState(
            enabled = intruder.enabled,
            switch = binding.intruderSwitch,
            stripe = binding.intruderStripe,
            content = binding.intruderContent,
            icon = binding.intruderIcon
        )
        binding.intruderThresholdItem.valueText = intruder.threshold.toString()
        binding.intruderWindowItem.valueText =
            getString(R.string.milliseconds_template, intruder.windowsMs)
    }

    private fun applySectionState(
        enabled: Boolean,
        switch: com.google.android.material.materialswitch.MaterialSwitch,
        stripe: View,
        content: ViewGroup,
        icon: android.widget.ImageView
    ) {
        switch.isChecked = enabled
        stripe.setBackgroundColor(
            ContextCompat.getColor(this, if (enabled) R.color.ui_green else R.color.ui_stroke)
        )
        content.alpha = if (enabled) 1f else DISABLED_SECTION_ALPHA
        // Section icon is green when the feature is active, gray when off.
        icon.setColorFilter(
            ContextCompat.getColor(
                this,
                if (enabled) R.color.ui_green else R.color.ui_text_secondary
            )
        )
    }

    private fun toggleSection(content: View, chevron: View) {
        val expand = content.visibility != View.VISIBLE
        content.visibility = if (expand) View.VISIBLE else View.GONE
        chevron.rotation = if (expand) 180f else 0f
    }

    private fun expandSection(content: View, chevron: View) {
        content.visibility = View.VISIBLE
        chevron.rotation = 180f
    }

    // Config changes are rejected by the firmware while recording/detecting; mirror the
    // gate used for the settings screen.
    private fun canEditConfig(): Boolean {
        if (DeviceDriver.session.recordingState.isInactive) {
            return true
        }
        showModalAlert(
            getString(R.string.not_available),
            getString(R.string.device_settings_not_available)
        )
        return false
    }

    private fun runCommand(command: Command) {
        binding.swipeRefreshLayout.isRefreshing = true
        DeviceDriver.processCommandQueue(command)
    }

    private fun setBoolConfigProperty(property: String, value: Boolean) {
        Command.createSetConfigPropertyCommand(
            property,
            value.toString(),
            ::runCommand,
            {
                showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
                refreshDeviceInfo()
            },
        ) { refreshDeviceInfo() }
    }

    private fun openTextEditor(
        property: String,
        settingName: String,
        currentValue: String,
        isNumeric: Boolean = false,
    ) {
        val intent = Intent(this, TextEditorActivity::class.java)
        intent.putExtra(BaseEditorActivity.EXTRA_SETTING_NAME, settingName)
        intent.putExtra(BaseEditorActivity.EXTRA_CURRENT_VALUE, currentValue)
        intent.putExtra(BaseEditorActivity.EXTRA_PROPERTY, property)
        intent.putExtra(BaseEditorActivity.EXTRA_IS_NUMERIC, isNumeric)
        intent.putExtra(BaseEditorActivity.EXTRA_PREFIX, "")
        startActivity(intent)
    }

    private fun setSectionListeners() {
        // LoRa
        binding.loraHeader.setOnClickListener {
            toggleSection(binding.loraContent, binding.loraChevron)
        }
        binding.loraHelpButton.setOnClickListener {
            showModalAlert(getString(R.string.lora), getString(R.string.help_lora))
        }
        binding.loraSwitch.setOnClickListener {
            val checked = binding.loraSwitch.isChecked
            if (canEditConfig()) {
                if (checked) {
                    expandSection(binding.loraContent, binding.loraChevron)
                }
                setBoolConfigProperty(LoraWan.ENABLED, checked)
            } else {
                binding.loraSwitch.isChecked = !checked
            }
        }
        binding.loraRegionItem.setOnClickListener {
            if (binding.loraSwitch.isChecked && canEditConfig()) {
                openTextEditor(
                    LoraWan.REGION,
                    getString(R.string.region),
                    DeviceDriver.lorawan.region,
                )
            }
        }
        binding.loraUplinkItem.setOnClickListener {
            if (binding.loraSwitch.isChecked && canEditConfig()) {
                val currentInterval = DeviceDriver.lorawan.uplinkIntervalSeconds
                RangeEditorActivity.openRangeEditor(
                    this,
                    LoraWan.UPLINK_INTERVAL,
                    getString(R.string.uplink_interval),
                    "$currentInterval (${prettifyTime(currentInterval)})",
                    currentInterval.toFloat(),
                    LoraWan.MIN_INTERVAL_SECS.toFloat(),
                    LoraWan.MAX_INTERVAL_SECS.toFloat()
                )
            }
        }

        // Scheduler (duty cycle)
        binding.schedulerHeader.setOnClickListener {
            toggleSection(binding.schedulerContent, binding.schedulerChevron)
        }
        binding.schedulerHelpButton.setOnClickListener {
            showModalAlert(getString(R.string.scheduler), getString(R.string.help_scheduler))
        }
        binding.schedulerSwitch.setOnClickListener {
            val checked = binding.schedulerSwitch.isChecked
            if (canEditConfig()) {
                if (checked) {
                    expandSection(binding.schedulerContent, binding.schedulerChevron)
                }
                setBoolConfigProperty(DutyCycle.ENABLE, checked)
            } else {
                binding.schedulerSwitch.isChecked = !checked
            }
        }
        binding.dutyAwakeItem.setOnClickListener {
            if (binding.schedulerSwitch.isChecked && canEditConfig()) {
                val current = DeviceDriver.dutyCycle.awakeDurationS
                RangeEditorActivity.openRangeEditor(
                    this,
                    DutyCycle.AWAKE_DURATION_S,
                    getString(R.string.duty_cycle_awake_duration),
                    "$current (${prettifyTime(current)})",
                    current.toFloat(),
                    DutyCycle.MIN_AWAKE_DURATION_S.toFloat(),
                    DutyCycle.MAX_AWAKE_DURATION_S.toFloat()
                )
            }
        }
        binding.dutySleepItem.setOnClickListener {
            if (binding.schedulerSwitch.isChecked && canEditConfig()) {
                val current = DeviceDriver.dutyCycle.sleepDurationS
                RangeEditorActivity.openRangeEditor(
                    this,
                    DutyCycle.SLEEP_DURATION_S,
                    getString(R.string.duty_cycle_sleep_duration),
                    "$current (${prettifyTime(current)})",
                    current.toFloat(),
                    DutyCycle.MIN_SLEEP_DURATION_S.toFloat(),
                    DutyCycle.MAX_SLEEP_DURATION_S.toFloat()
                )
            }
        }

        // Intruder detection
        binding.intruderHeader.setOnClickListener {
            toggleSection(binding.intruderContent, binding.intruderChevron)
        }
        binding.intruderHelpButton.setOnClickListener {
            showModalAlert(
                getString(R.string.intruder_detection),
                getString(R.string.help_intruder)
            )
        }
        binding.intruderSwitch.setOnClickListener {
            val checked = binding.intruderSwitch.isChecked
            if (canEditConfig()) {
                if (checked) {
                    expandSection(binding.intruderContent, binding.intruderChevron)
                }
                setBoolConfigProperty(Intruder.ENABLED, checked)
            } else {
                binding.intruderSwitch.isChecked = !checked
            }
        }
        binding.intruderThresholdItem.setOnClickListener {
            if (binding.intruderSwitch.isChecked && canEditConfig()) {
                openTextEditor(
                    Intruder.THRESHOLD,
                    getString(R.string.intruder_threshold),
                    DeviceDriver.intruder.threshold.toString(),
                    true,
                )
            }
        }
        binding.intruderWindowItem.setOnClickListener {
            if (binding.intruderSwitch.isChecked && canEditConfig()) {
                openTextEditor(
                    Intruder.WINDOWS_MS,
                    getString(R.string.intruder_windows_ms),
                    DeviceDriver.intruder.windowsMs.toString(),
                    true,
                )
            }
        }

        // Device details (plain expander, no switch)
        binding.detailsHeader.setOnClickListener {
            toggleSection(binding.detailsContent, binding.detailsChevron)
        }
    }

    private fun elocFound(device: BtDevice) {
        if (device.address == deviceAddress) {
            elocReceiver.unregister(this)
            BluetoothHelper.scanningSpecificEloc = false
            setViewMode(ViewMode.ConnectingView)
            connectToDevice(fallbackToScanOnFailure = false)
        }
    }

    private fun scanForDevice() {
        // Register for device found events
        elocReceiver.register(this)

        // Do a scan to find the required device and only connect when device is found.
        setViewMode(ViewMode.ScanningView)
        BluetoothHelper.scanningSpecificEloc = true
        BluetoothHelper.startScan({ remaining ->
            runOnUiThread {
                if (remaining <= 0) {
                    DeviceDriver.connect(deviceAddress, null) {
                        BluetoothHelper.scanningSpecificEloc = false
                        showModalAlert(
                            getString(R.string.eloc_not_found),
                            getString(R.string.eloc_not_found_rationale)
                        ) { goBack() }
                    }
                }
            }
        }) {
            runOnUiThread {
                if (it != null) {
                    showModalAlert(getString(R.string.bluetooth), it)
                }
            }
        }

    }

    private fun setListeners() {
        DeviceDriver.addConnectionChangedListener(listenerId, ::onConnectionStatusChanged)
        binding.skipButton.setOnClickListener { skipTimeSync() }
        binding.syncTimeButton.setOnClickListener { manualTimeSync() }
        binding.modeButton.addClickListener { modeButtonClicked() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.backButton.setOnClickListener { goBack() }
        setSectionListeners()
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.mnu_settings -> openSettings()
                R.id.mnu_reboot -> confirmReboot()
            }
            return@setOnMenuItemClickListener true
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!refreshing) {
                refreshing = true
                connectToDevice()
            }
        }

        // The SwipeRefreshLayout's direct child is a (non-scrollable) ConstraintLayout,
        // so its default canChildScrollUp() always reports "at top" and every downward
        // drag looks like a pull-to-refresh. Ask the real ScrollView instead, so refresh
        // only arms when the content is genuinely scrolled to the very top.
        binding.swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            binding.scrollView.canScrollVertically(-1)
        }
    }

    // The firmware replies first and restarts ~1 s later; the resulting Bluetooth
    // disconnect is handled by onConnectionStatusChanged (Disconnected view).
    private fun confirmReboot() {
        showModalOptionAlert(
            title = getString(R.string.reboot_device),
            message = getString(R.string.reboot_device_message),
            positiveButtonLabel = getString(R.string.reboot_now),
            negativeButtonLabel = getString(R.string.cancel),
            positiveCallback = {
                DeviceDriver.processCommandQueue(Command.createRebootCommand { })
            },
        )
    }

    private fun openSettings(showMicrophoneSection: Boolean = false) {
        if (DeviceDriver.session.recordingState.isInactive) {
            val intent = Intent(this, DeviceSettingsActivity::class.java)
            intent.putExtra(DeviceSettingsActivity.EXTRA_SHOW_RECORDER, showMicrophoneSection)
            startActivity(intent)
        } else {
            showModalAlert(
                getString(R.string.not_available),
                getString(R.string.device_settings_not_available)
            )
        }
    }

    private fun connectToDevice(fallbackToScanOnFailure: Boolean = true) {
        timeSyncHandled = false
        showDeviceInfoStarted = false
        gpsLocationUpdate = null
        setViewMode(ViewMode.ConnectingView)
        DeviceDriver.connect(deviceAddress) {
            runOnUiThread {
                if (fallbackToScanOnFailure) {
                    // Direct connect failed (device may be out of range or stale bond
                    // info). Fall back to discovery scan as a recovery path.
                    scanForDevice()
                } else if (it.isNotEmpty()) {
                    showModalAlert(getString(R.string.bluetooth), it)
                }
            }
        }
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun synchronizeTime() {
        // Skip the setTime round-trip when the device's on-board GPS has already
        // synced the clock AND has a position fix (the fix lets the firmware derive
        // the timezone from longitude). Sending phone time in that state would only
        // replace GPS-disciplined time with phone time and permanently pin the
        // phone's timezone, disabling the GPS timezone self-localisation.
        // The manual sync button on the status page remains the explicit override.
        //
        // V1.54 made getStatus "hasFix" live-gated, so the old timeSynced+hasFix heuristic no longer
        // works at connect time (the GPS may not hold a live fix at that instant even though it is
        // GPS-disciplined). Prefer the firmware's own persisted markers when present: skip the sync
        // only when the clock is GPS-set AND the timezone is not still the compile-time default.
        // Old firmware (no timeSource) falls back to the original heuristic.
        val gpsHandlesTime = if (DeviceDriver.general.deviceTimeSource.isNotBlank()) {
            DeviceDriver.general.deviceTimeSource == "gps" &&
                DeviceDriver.general.deviceTzSource != "default"
        } else {
            DeviceDriver.gps.timeSynced && DeviceDriver.gps.hasFix   // old-firmware fallback
        }
        if (timeSyncHandled || gpsHandlesTime) {
            timeSyncHandled = true
            continueDeviceInfo()
        } else {
            setViewMode(ViewMode.SyncingTimeView)
            DeviceDriver.syncTime { onTimeSyncCompleted() }
        }
    }

    // Manual "sync now" from the status page: always sends phone time + timezone,
    // regardless of the device's GPS state. This is the deliberate override for the
    // automatic skip in synchronizeTime() — note it permanently pins the phone's
    // timezone on the device (GPS longitude-derived TZ is disabled from then on).
    private fun manualTimeSync() {
        setSyncTimeButtonBusy(true)
        DeviceDriver.syncTime {
            runOnUiThread {
                setSyncTimeButtonBusy(false)
                // Re-read status so the displayed ELOC time reflects the sync;
                // the get-command listener rebinds the UI when the response arrives.
                DeviceDriver.getStatus(saveToDatabase = false) { }
            }
        }
    }

    private fun setSyncTimeButtonBusy(busy: Boolean) {
        binding.syncTimeButton.isEnabled = !busy
        binding.syncTimeButton.alpha = if (busy) DISABLED_SECTION_ALPHA else 1f
    }

    private fun setViewMode(mode: ViewMode) {
        runOnUiThread {
            when (mode) {
                ViewMode.ScanningView -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.scanning_single_eloc_device)
                }

                ViewMode.ConnectingView -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.connecting)
                }

                ViewMode.LocationPendingView -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.wait_for_location)
                }

                ViewMode.FetchingDataView -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.getting_eloc_info)
                }

                ViewMode.SyncingTimeView -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.VISIBLE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.syncing_time)
                }

                ViewMode.ContentView -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.initLayout.visibility = View.GONE
                }

                ViewMode.SettingRecordMode -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.setting_mode)
                }

                ViewMode.StoppingRecordMode -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.stopping)
                }

                ViewMode.Disconnected -> {
                    binding.toolbar.visibility = View.GONE
                    binding.contentLayout.visibility = View.GONE
                    binding.initLayout.visibility = View.VISIBLE
                    binding.skipButton.visibility = View.GONE
                    binding.progressIndicator.infoMode = true
                    binding.progressIndicator.text =
                        getString(R.string.disconnected_swipe_to_reconnect)
                }
            }
        }
    }

    private enum class GpsChoice { PHONE, ELOC, NONE }

    // The more accurate of the two fixes: the phone's Location.accuracy vs the ELOC's HDOP-derived
    // accuracy (both meters, both capped at 100 m to reject wild values). The ELOC only qualifies
    // when it has a live fix AND real coordinates to record. Old firmware reports hdop 0.0 ⇒
    // accuracyMeters -1.0 (and no lat/lon) ⇒ the ELOC branch never wins (phone-only, as before).
    private fun chooseGpsSource(): GpsChoice {
        val phone = gpsLocationUpdate?.accuracy?.toDouble()?.takeIf { it in 0.0..100.0 }
        val eloc = DeviceDriver.gps.let {
            if (it.hasFix && it.hasLocation) it.accuracyMeters.takeIf { m -> m in 0.0..100.0 } else null
        }
        return when {
            phone != null && (eloc == null || phone <= eloc) -> GpsChoice.PHONE
            eloc != null -> GpsChoice.ELOC
            else -> GpsChoice.NONE
        }
    }

    // The location that should actually be recorded/uploaded: the ELOC's own fix when it wins the
    // accuracy comparison, otherwise the phone's fix (unchanged from the historical behaviour). This
    // feeds both the setLocation command sent to the device and the Firebase upload.
    private fun effectiveGpsData(): GpsData? = when (chooseGpsSource()) {
        GpsChoice.ELOC -> DeviceDriver.gps.let {
            GpsData(
                accuracy = it.accuracyMeters.roundToInt(),
                source = GpsDataSource.Radio,
                latitude = it.latitude,
                longitude = it.longitude,
            )
        }
        GpsChoice.PHONE, GpsChoice.NONE -> gpsLocationUpdate
    }

    // Merged accuracy gauge: shows whichever source chooseGpsSource() prefers, labelling it.
    private fun updateGpsViews() {
        val phone = gpsLocationUpdate?.accuracy?.toDouble()?.takeIf { it in 0.0..100.0 }
        val eloc = DeviceDriver.gps.accuracyMeters.takeIf { it in 0.0..100.0 }
        val best: Double?
        val sourceRes: Int
        when (chooseGpsSource()) {
            GpsChoice.PHONE -> {
                best = phone
                sourceRes = R.string.accuracy_source_phone
            }
            GpsChoice.ELOC -> {
                best = eloc
                sourceRes = R.string.accuracy_source_eloc
            }
            GpsChoice.NONE -> {
                best = null
                sourceRes = R.string.accuracy_source_phone
            }
        }
        binding.gpsGauge.updateValue(best ?: -1.0)  // negative ⇒ errorMode (unchanged)
        val units = "m"
        if (best != null) {
            binding.gpsAccuracyTextView.text = formatNumber(best.toInt(), units, units, 0)
            binding.gpsAccuracyTextView.visibility = View.VISIBLE
            binding.gpsSubLabel.text = getString(sourceRes)
            binding.gpsSubLabel.visibility = View.VISIBLE
            binding.gpsNoAccuracyImageView.visibility = View.GONE
        } else {
            binding.gpsAccuracyTextView.visibility = View.GONE
            binding.gpsSubLabel.visibility = View.GONE
            binding.gpsNoAccuracyImageView.visibility =
                if (gpsLocationUpdate == null) View.GONE else View.VISIBLE
        }
    }

    private fun showSDCardError() {
        if (hasSDCardError) {
            showModalAlert(
                getString(R.string.sd_card),
                getString(R.string.sd_card_error_message),
            )
        }
    }

    private fun confirmIgnoreAccuracy(state: RecordState) {
        showModalOptionAlert(
            getString(R.string.confirm),
            getString(R.string.please_wait_for_gps_accuracy),
            getString(R.string.record_anyway),
            getString(R.string.wait55),
            { setRecordingState(state, true) },
        )
    }

    private fun modeButtonClicked() {
        if (!binding.modeButton.isBusy) {
            openModeChooser()
        }
    }

    private fun openModeChooser() {
        val modeSheet = BottomSheetDialog(this)
        val modeSheetBinding = LayoutModeChooserBinding.inflate(layoutInflater)
        modeSheetBinding.backButton.setOnClickListener {
            modeSheet.dismiss()
        }
        modeSheetBinding.recordButton.setOnClickListener {
            modeSheet.dismiss()
            setRecordingState(RecordState.RecordOnDetectOff)
        }
        modeSheetBinding.recordWithAiButton.setOnClickListener {
            modeSheet.dismiss()
            setRecordingState(RecordState.RecordOnDetectOn)
        }
        modeSheetBinding.aiDetectionButton.setOnClickListener {
            modeSheet.dismiss()
            setRecordingState(RecordState.RecordOffDetectOn)
        }
        modeSheetBinding.recordOnEventButton.setOnClickListener {
            modeSheet.dismiss()
            setRecordingState(RecordState.RecordOnEvent)
        }
        modeSheetBinding.stopButton.setOnClickListener {
            modeSheet.dismiss()
            setRecordingState(RecordState.RecordOffDetectOff, true)
        }
        modeSheet.setContentView(modeSheetBinding.root)
        // The sheet container has its own opaque background; clear it so the rounded
        // top corners of our layout are visible.
        (modeSheetBinding.root.parent as? View)?.setBackgroundColor(
            android.graphics.Color.TRANSPARENT
        )
        modeSheet.setCancelable(true)
        modeSheet.show()
    }

    private fun setRecordingState(newMode: RecordState, ignoreLocationAccuracy: Boolean = false) {
        if (newMode == DeviceDriver.session.recordingState) {
            return
        }

        if (hasSDCardError && (newMode != RecordState.Invalid) && (newMode != RecordState.RecordOffDetectOff)) {
            showSDCardError()
            return
        }


        /*
        // Allow recording, even without a valid location
        if (!LocationHelper.isValidLocationCode(locationCode)) {
            showModalAlert(
                getString(R.string.location_required),
                getString(R.string.wait_for_location)
            )
            return
        }
        */

        // Gate on the accuracy of the source we would actually record (best of phone / ELOC fix),
        // and record that same fix.
        val recordLocation = effectiveGpsData()
        val accuracy = recordLocation?.accuracy ?: -1
        if (ignoreLocationAccuracy || (accuracy <= 8.1)) {
            var proceed = true

            val task = {
                if (proceed) {
                    binding.modeButton.busyText = R.string.setting_mode
                    binding.modeButton.setButtonColor(R.color.detecting_off_button)
                    binding.modeButton.isBusy = true

                    val viewMode = when (newMode) {
                        RecordState.RecordOffDetectOff -> ViewMode.StoppingRecordMode
                        else -> ViewMode.SettingRecordMode
                    }
                    setViewMode(viewMode)
                    DeviceDriver.setRecordState(newMode, recordLocation) { }
                }
            }
            if (DeviceDriver.bluetooth.enableDuringRecord) {
                task()
            } else {
                showModalOptionAlert(
                    getString(R.string.important),
                    getString(R.string.connection_close_message),
                    positiveButtonLabel = getString(R.string.yes_continue),
                    positiveCallback = {
                        task()
                    },
                    negativeCallback = {
                        proceed = false
                        task()
                    }
                )
            }
        } else {
            confirmIgnoreAccuracy(newMode)
        }
    }

    private fun displayRecordingState(errorMessage: String = "") {
        val err = errorMessage.trim()
        val state = DeviceDriver.session.recordingState
        binding.modeButton.text = state.getVerb()
        binding.modeButton.isBusy = false
        val backColor = if (state == RecordState.RecordOffDetectOff) {
            R.color.recording_off_button
        } else {
            R.color.recording_on_button
        }
        binding.modeButton.setButtonColor(backColor)
        if (err.isNotEmpty()) {
            showModalAlert(getString(R.string.oops), err)
        }
    }
}
