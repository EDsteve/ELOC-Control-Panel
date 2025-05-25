package de.eloc.eloc_control_panel.activities.themable

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.formatNumber
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.onWriteCommandError
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.data.BtDevice
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
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback
import de.eloc.eloc_control_panel.receivers.ElocReceiver
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

// todo: add refresh menu item for old API levels

class DeviceActivity : ThemableActivity() {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
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

    private var checkingGpsTimeoutStarted = false
    private var checkingGpsTimeoutCompleted = false
    private val listenerId = "deviceActivity"
    private var hasSDCardError = false
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var timeSyncHandled = false
    private var refreshing = false
    private var paused = false
    private var statusReceived = false
    private var configReceived = false
    private var gpsLocationUpdate: GpsData? = null
    private val scrollChangeListener =
        View.OnScrollChangeListener { _, _, y, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (y <= 5)
        }
    private lateinit var elocReceiver: ElocReceiver

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
                        DeviceDriver.getStatus { }
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
    }

    override fun onPause() {
        super.onPause()
        paused = true
        LocationHelper.stopUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsLocationUpdate = null
        DeviceDriver.removeConnectionChangedListener(listenerId)
        DeviceDriver.removeWriteCommandLister(listenerId)
        DeviceDriver.removeOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.removeOnGetCommandCompletedListener(getCommandCompletedListener)
        DeviceDriver.disconnect()
    }

    private fun checkGpsLocationTimeout() {
        if (!checkingGpsTimeoutStarted) {
            checkingGpsTimeoutStarted = true
            checkingGpsTimeoutCompleted = false
            Executors.newSingleThreadExecutor().execute {
                var waitTime = Preferences.gpsLocationTimeoutSeconds
                while ((waitTime > 0) && (!paused) && (gpsLocationUpdate == null)) {
                    try {
                        Thread.sleep(1000)
                    } catch (_: Exception) {
                    }
                    waitTime--
                }
                checkingGpsTimeoutCompleted = true
                showDeviceInfo()
            }
        }
    }

    private fun onConnectionStatusChanged(status: ConnectionStatus) {
        runOnUiThread {
            refreshing = false
            binding.swipeRefreshLayout.isRefreshing = false
            binding.toolbar.menu.clear()
            when (status) {
                ConnectionStatus.Active -> synchronizeTime()

                ConnectionStatus.Inactive -> {
                    binding.swipeRefreshLayout.isEnabled = true
                    timeSyncHandled = false
                    setViewMode(ViewMode.Disconnected)
                }

                ConnectionStatus.Pending -> {
                    timeSyncHandled = false
                    binding.swipeRefreshLayout.isEnabled = false
                    setViewMode(ViewMode.ConnectingView)
                }
            }
        }
    }

    private fun initialize() {
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
                binding.appVersionItem.valueText = App.versionName
                updateGpsViews()
                setListeners()
                scanForDevice()
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
        timeSyncHandled = true
        showDeviceInfo()
    }

    private fun showDeviceInfo() {
        runOnUiThread {
            if (timeSyncHandled) {
                if (gpsLocationUpdate == null && (!checkingGpsTimeoutCompleted)) {
                    setViewMode(ViewMode.LocationPendingView)
                } else {
                    DeviceDriver.getStatus {
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
                            val timeout = Preferences.gpsLocationTimeoutSeconds
                            val source = gpsLocationUpdate?.source ?: GpsDataSource.Unknown
                            val message: String? = when (source) {
                                GpsDataSource.Unknown -> getString(
                                    R.string.gps_unknown_coordinates,
                                    timeout
                                )

                                GpsDataSource.Cache -> getString(
                                    R.string.gps_cached_coordinates,
                                    timeout
                                )

                                GpsDataSource.Radio -> null
                            }

                            if (message != null) {
                                showModalAlert(
                                    getString(R.string.gps_coordinates),
                                    message
                                ) { onFirstLocationReceived() }
                            } else {
                                onFirstLocationReceived()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onFirstLocationReceived() {
        setViewMode(ViewMode.FetchingDataView)
        DeviceDriver.getElocInformation(gpsLocationUpdate, true) {
            val type = DeviceDriver.getCommandType(it)
            checkReceivedInfoType(type)
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
            menuInflater.inflate(R.menu.app_bar_settings, binding.toolbar.menu)

            try {
                // Make settings icon larger (at least 128x128)
                val settingsItem = binding.toolbar.menu.findItem(R.id.mnu_settings)
                val oldIcon = settingsItem.icon
                if (oldIcon != null) {
                    val newSize = 128
                    val bitmap = createBitmap(newSize, newSize)
                    val canvas = Canvas(bitmap)
                    oldIcon.bounds = Rect(0, 0, newSize, newSize)
                    oldIcon.draw(canvas)
                    settingsItem.icon = bitmap.toDrawable(resources)
                }
            } catch (_: Exception) {
            }

            setConfigInfo()
            setStatusInfo()
            setViewMode(ViewMode.ContentView)
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
        binding.detectedEventsItem.valueText = DeviceDriver.session.eventsDetected.toString()
        binding.aiModelItem.valueText = DeviceDriver.session.aiModel
        binding.recSinceBootItem.valueText =
            TimeHelper.formatHours(this, DeviceDriver.general.recHoursSinceBoot)
        binding.firmwareVersionItem.valueText = DeviceDriver.general.version
        binding.uptimeItem.valueText =
            TimeHelper.formatHours(this, DeviceDriver.general.uptimeHours)
        val level = DeviceDriver.battery.level
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batteryGauge.updateValue(level)
        binding.batteryVoltItem.valueText = formatNumber(DeviceDriver.battery.voltage, "V")
        binding.batteryTypeItem.valueText = DeviceDriver.battery.type
        val free = DeviceDriver.sdCard.freeGb
        binding.storageGauge.errorMode = (free <= 0)
        binding.storageStatus.text = if (binding.storageGauge.errorMode)
            ""
        else
            getString(R.string.gauge_free_space, free.toInt())
        binding.storageGauge.updateValue(DeviceDriver.sdCard.freePercentage)
        val gb = DeviceDriver.sdCard.sizeGb
        if (gb <= 0.0) {
            binding.storageTextView.text = getString(R.string.no_sd_card)
            binding.storageGauge.updateValue(0.0)
        } else {
            binding.storageTextView.text = formatNumber(gb, "GB")
        }
        displayRecordingState()
    }

    private fun setConfigInfo() {
        binding.toolbar.title = DeviceDriver.general.nodeName
        binding.sampleRateItem.valueText = DeviceDriver.microphone.sampleRate.toString()
        binding.microphoneTypeItem.valueText = DeviceDriver.microphone.type
        binding.gainItem.valueText = DeviceDriver.microphone.volumePower.percentage
        binding.timePerFileItem.valueText = DeviceDriver.general.timePerFile.toString()
        binding.lastLocationItem.valueText = DeviceDriver.general.lastLocation
        val enabledLabel =
            getString(if (DeviceDriver.bluetooth.enableDuringRecord) R.string.on else R.string.off)
        binding.bluetoothDuringRecordingItem.valueText = enabledLabel
        binding.fileHeaderItem.valueText = DeviceDriver.general.fileHeader
    }

    private fun elocFound(device: BtDevice) {
        if (device.address == deviceAddress) {
            elocReceiver.unregister(this)
            BluetoothHelper.scanningSpecificEloc = false
            setViewMode(ViewMode.ConnectingView)
            connectToDevice()
        }
    }

    private fun scanForDevice() {
        // Register for device found events
        elocReceiver.register(this)
        gpsLocationUpdate = null
        checkGpsLocationTimeout()

        // Do a scan to find the required device and only connecBluetootht when device is found.
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
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.modeButton.addClickListener { modeButtonClicked() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.backButton.setOnClickListener { goBack() }
        binding.recorderContainer.setOnClickListener {
            openSettings(true)
        }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.mnu_settings) {
                openSettings()
            }
            return@setOnMenuItemClickListener true
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!refreshing) {
                refreshing = true
                scanForDevice()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.scrollView.setOnScrollChangeListener(scrollChangeListener)
        }
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

    private fun connectToDevice() {
        timeSyncHandled = false
        DeviceDriver.connect(deviceAddress) {
            runOnUiThread {
                if (it?.isNotEmpty() == true) {
                    showModalAlert(getString(R.string.bluetooth), it)
                }
            }
        }
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun synchronizeTime() {
        if (timeSyncHandled) {
            onTimeSyncCompleted()
        } else {
            setViewMode(ViewMode.SyncingTimeView)
            DeviceDriver.syncTime { onTimeSyncCompleted() }
        }
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

    private fun updateGpsViews() {
        binding.gpsGauge.updateValue(gpsLocationUpdate?.accuracy?.toDouble() ?: -1.0)
        val accuracy = gpsLocationUpdate?.accuracy ?: -1
        binding.gpsAccuracyTextView.text = formatNumber(accuracy, "m", 0)
        if ((accuracy >= 0) && (accuracy <= 100)) {
            binding.gpsAccuracyTextView.visibility = View.VISIBLE
            binding.gpsNoAccuracyImageView.visibility = View.GONE
        } else {
            binding.gpsAccuracyTextView.visibility = View.GONE
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

        val accuracy = gpsLocationUpdate?.accuracy ?: -1
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
                    DeviceDriver.setRecordState(newMode, gpsLocationUpdate) { }
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