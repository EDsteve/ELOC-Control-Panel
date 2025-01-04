package de.eloc.eloc_control_panel.activities.themable

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.formatNumber
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.databinding.LayoutModeChooserBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.interfaces.ConnectionStatusListener
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback
import de.eloc.eloc_control_panel.receivers.ElocReceiver

// todo: add refresh menu item for old API levels

class DeviceActivity : ThemableActivity(), ConnectionStatusListener {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private enum class ViewMode {
        ScanningView,
        ConnectingView,
        SyncingTimeView,
        ContentView,
        Disconnected,
    }

    private var hasSDCardError = false
    private var locationAccuracy = 100.0 // Start with very inaccurate value of 100 meters.
    private var locationCode = LocationHelper.UNKNOWN
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var firstResume = true
    private var timeSyncHandled = false
    private var refreshing = false
    private var paused = false
    private val scrollChangeListener =
        View.OnScrollChangeListener { _, _, y, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (y <= 5)
        }
    private lateinit var elocReceiver: ElocReceiver

    private val getCommandCompletedListener = GetCommandCompletedCallback {
        runOnUiThread {
            // Hide the progress indicator when data is received i.e., there is a connection
            binding.swipeRefreshLayout.isRefreshing = false

            when (it) {
                CommandType.GetStatus -> setStatusData()
                CommandType.GetConfig -> setConfigData()
                else -> {}
            }
        }
    }

    private val setCommandCompletedCallback = SetCommandCompletedCallback { success, type ->
        runOnUiThread {
            when (type) {
                CommandType.SetRecordMode -> {
                    setRecordingState()
                    if (success) {
                        DeviceDriver.getStatus()
                    }
                }

                CommandType.SetTime -> timeSyncCompleted()

                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        DeviceDriver.addOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.addOnGetCommandCompletedListener(getCommandCompletedListener)
        setRecordingState()
        initialize()
    }

    override fun onResume() {
        super.onResume()
        paused = false
        if (firstResume) {
            firstResume = false
        } else {
            setConfigData()
            setStatusData()
        }
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        paused = true
        LocationHelper.stopUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.clearConnectionStatusListener()
        DeviceDriver.removeOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.removeOnGetCommandCompletedListener(getCommandCompletedListener)
        DeviceDriver.disconnect()
    }

    override fun onStatusChanged(status: ConnectionStatus) {
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
        elocReceiver = ElocReceiver(null) { elocFound(it) }
        onStatusChanged(ConnectionStatus.Inactive)
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

    private fun skipTimeSync() {
        showModalAlert(
            getString(R.string.time_sync),
            getString(R.string.time_sync_rationale),
        ) {
            timeSyncCompleted()
        }
    }

    private fun timeSyncCompleted() {
        timeSyncHandled = true
        binding.swipeRefreshLayout.isEnabled = true
        setViewMode(ViewMode.ContentView)
        binding.toolbar.menu.clear()
        menuInflater.inflate(R.menu.app_bar_settings, binding.toolbar.menu)
        DeviceDriver.getStatusAndConfig(true)
    }

    private fun setStatusData() {
        binding.sessionIdItem.valueText = DeviceDriver.session.ID
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
        setRecordingState()
    }

    private fun setConfigData() {
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

        // Do a scan to find the required device and only connect when device is found.
        setViewMode(ViewMode.ScanningView)
        BluetoothHelper.scanningSpecificEloc = true
        BluetoothHelper.startScan { remaining ->
            runOnUiThread {
                if (remaining <= 0) {
                    BluetoothHelper.scanningSpecificEloc = false
                    showModalAlert(
                        getString(R.string.eloc_not_found),
                        getString(R.string.eloc_not_found_rationale)
                    ) { goBack() }
                }
            }
        }
    }

    private fun setListeners() {
        DeviceDriver.registerConnectionStatusListener(this)
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
        DeviceDriver.connect(deviceAddress)
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun synchronizeTime() {
        if (timeSyncHandled) {
            timeSyncCompleted()
        } else {
            setViewMode(ViewMode.SyncingTimeView)
            TimeHelper.syncBoardClock()
        }
    }

    private fun setViewMode(mode: ViewMode) {
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

    private fun startLocationUpdates() {
        val listener = object : LocationListener {
            @Deprecated("Deprecated in Java")
            @Suppress("deprecated")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onLocationChanged(location: Location) {
                locationAccuracy = location.accuracy.toDouble()
                val code = OpenLocationCode(location.latitude, location.longitude)
                locationCode = code.code
                updateGpsViews()
            }
        }

        LocationHelper.startUpdates(listener)
    }

    private fun updateGpsViews() {
        binding.gpsGauge.updateValue(locationAccuracy)
        binding.gpsStatus.text = formatNumber(locationAccuracy, "m", 0)
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
            { requestState(state, true) },
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
            requestState(RecordState.RecordOnDetectOff)
        }
        modeSheetBinding.recordWithAiButton.setOnClickListener {
            modeSheet.dismiss()
            requestState(RecordState.RecordOnDetectOn)
        }
        modeSheetBinding.aiDetectionButton.setOnClickListener {
            modeSheet.dismiss()
            requestState(RecordState.RecordOffDetectOn)
        }
        modeSheetBinding.recordOnEventButton.setOnClickListener {
            modeSheet.dismiss()
            requestState(RecordState.RecordOnEvent)
        }
        modeSheetBinding.stopButton.setOnClickListener {
            modeSheet.dismiss()
            requestState(RecordState.RecordOffDetectOff, true)
        }
        modeSheet.setContentView(modeSheetBinding.root)
        modeSheet.setCancelable(true)
        modeSheet.show()
    }

    private fun requestState(newMode: RecordState, ignoreLocationAccuracy: Boolean = false) {
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

        if (ignoreLocationAccuracy || (locationAccuracy <= 8.1)) {
            var proceed = true

            val task = {
                if (proceed) {
                    binding.modeButton.busyText = R.string.setting_mode
                    binding.modeButton.setButtonColor(R.color.detecting_off_button)
                    binding.modeButton.isBusy = true

                    when (newMode) {
                        RecordState.RecordOffDetectOff -> DeviceDriver.setRecordState(newMode)
                        else -> {
                            DeviceDriver.setRecordState(
                                newMode,
                                locationCode,
                                locationAccuracy
                            )
                        }
                    }
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

    private fun setRecordingState(errorMessage: String = "") {
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