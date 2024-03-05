package de.eloc.eloc_control_panel.activities

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AppState
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.databinding.LayoutModeChooserBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback

// todo: add refresh menu item for old API levels

class DeviceActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private var hasSDCardError = false
    private var locationAccuracy = 100.0 // Start with very inaccurate value of 100 meters.
    private var locationCode = LocationHelper.UNKNOWN
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var firstResume = true
    private var refreshing = false
    private val scrollChangeListener =
        View.OnScrollChangeListener { _, _, y, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (y <= 5)
        }

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

                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        DeviceDriver.addOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.addOnGetCommandCompletedListener(getCommandCompletedListener)
        setContentView(binding.root)
        setRecordingState()
        initialize()
    }

    override fun onResume() {
        super.onResume()
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
        LocationHelper.stopUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.removeOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.removeOnGetCommandCompletedListener(getCommandCompletedListener)
        DeviceDriver.disconnect()
    }

    private fun initialize() {
        onConnectionChanged(ConnectionStatus.Inactive)
        val extras = intent.extras
        deviceAddress = extras?.getString(EXTRA_DEVICE_ADDRESS, "")?.trim() ?: ""
        if (deviceAddress.isEmpty()) {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.device_address_required),
                ::goBack
            )
        } else {
            if (!AppState.hasValidProfile) {
                showModalAlert(
                    getString(R.string.required),
                    getString(R.string.ranger_name_required),
                    ::goBack
                )
            } else {
                binding.toolbar.title = getString(R.string.getting_node_name)
                binding.toolbar.subtitle = getString(R.string.eloc_user, AppState.rangerName)
                binding.appVersionItem.valueText = App.versionName
                updateGpsViews()
                setListeners()
                connect()
            }
        }
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
        binding.gainItem.valueText = DeviceDriver.microphone.gain.toString()
        binding.timePerFileItem.valueText = DeviceDriver.general.timePerFile.toString()
        binding.lastLocationItem.valueText = DeviceDriver.general.lastLocation
        val enabledLabel =
            getString(if (DeviceDriver.bluetooth.enableDuringRecord) R.string.on else R.string.off)
        binding.bluetoothDuringRecordingItem.valueText = enabledLabel
        binding.fileHeaderItem.valueText = DeviceDriver.general.fileHeader
    }

    private fun connect() {
        binding.toolbar.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
        binding.loadingLinearLayout.visibility = View.VISIBLE
        binding.progressIndicator.visibility = View.VISIBLE
        binding.progressIndicator.text = getString(R.string.connecting)
        connectToDevice()
    }

    private fun setListeners() {
        DeviceDriver.registerConnectionChangedListener { onConnectionChanged(it) }
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
                connectToDevice()
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
        DeviceDriver.connect(deviceAddress)
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun onConnectionChanged(status: ConnectionStatus) {
        runOnUiThread {
            refreshing = false
            binding.swipeRefreshLayout.isRefreshing = false
            binding.toolbar.menu.clear()
            when (status) {
                ConnectionStatus.Active -> {
                    binding.swipeRefreshLayout.isEnabled = true
                    toggleContent(true)
                    menuInflater.inflate(R.menu.app_bar_settings, binding.toolbar.menu)
                    DeviceDriver.getDeviceInfo(true)
                }

                ConnectionStatus.Inactive -> {
                    binding.swipeRefreshLayout.isEnabled = true
                    toggleContent(false)
                    binding.progressIndicator.infoMode = true
                    binding.progressIndicator.text =
                        getString(R.string.disconnected_swipe_to_reconnect)
                }

                ConnectionStatus.Pending -> {
                    binding.swipeRefreshLayout.isEnabled = false
                    toggleContent(false)
                    binding.progressIndicator.infoMode = false
                    binding.progressIndicator.text = getString(R.string.connecting)
                }
            }
        }
    }

    private fun toggleContent(visible: Boolean) {
        if (visible) {
            binding.toolbar.visibility = View.VISIBLE
            binding.contentLayout.visibility = View.VISIBLE
            binding.loadingLinearLayout.visibility = View.GONE
        } else {
            binding.toolbar.visibility = View.GONE
            binding.contentLayout.visibility = View.GONE
            binding.loadingLinearLayout.visibility = View.VISIBLE
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