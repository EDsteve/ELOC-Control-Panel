package de.eloc.eloc_control_panel.activities

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback
import de.eloc.eloc_control_panel.interfaces.SetCommandCompletedCallback

// todo: show free space text under storage gauge
// todo: show % of free storage in gauge after firmware update
// todo: show battery % after firmware update
// todo: STATUS section will have data loaded after firmware update.
// todo: DETECTOR SETTINGS section -> RecordWhenDetected, Model, Communication -> after AI has been implemented
// todo: detecting button to be activated when detection is implemented.
// todo: add refresh menu item for old API levels
// todo: is LabelColor enum used?

class DeviceActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_RANGER_NAME = "ranger_name"
        private const val CMD_SET_RECORD_MODE = "setRecordMode"
    }

    private var hasSDCardError = false
    private var locationAccuracy = 100.0 // Start with very inaccurate value of 100 meters.
    private var locationCode = "UNKNOWN"
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var rangerName = ""
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

    private val setCommandCompletedCallback = SetCommandCompletedCallback {success, type ->
        runOnUiThread {
            when(type) {
                CommandType.SetRecordMode -> {
                    setDeviceState(DeviceDriver.general.recordingState)
                    if (success) {
                        DeviceDriver.getStatus()
                    }
                }
                else -> {}
            }
        }
    }

    private var recordingState = RecordState.Invalid
        set(value) {
            field = value
            // todo: update recording/detecting since labels correctly
            binding.stopButton.visibility = View.GONE
            binding.startRecordingButton.visibility = View.GONE
            binding.startDetectingButton.visibility = View.GONE
            binding.stopButton.isClickable = false
            binding.stopButton.isFocusable = false
            when (field) {
                RecordState.Invalid, RecordState.RecordOffDetectOff -> {
                    binding.startRecordingButton.visibility = View.VISIBLE
                    binding.startDetectingButton.visibility = View.VISIBLE
                }

                else -> {
                    binding.stopButton.text = getString(R.string.stop_recording)
                    binding.stopButton.visibility = View.VISIBLE
                    binding.stopButton.isClickable = true
                    binding.stopButton.isFocusable = true
                }


                // todo fix enum values
                /*
                DeviceState.Detecting -> {
                    binding.stopButton.text = getString(R.string.stop_detecting)
                    binding.stopButton.visibility = View.VISIBLE
                    binding.stopButton.isClickable = true
                    binding.stopButton.isFocusable = true
                }*/
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        DeviceDriver.addOnSetCommandCompletedListener(setCommandCompletedCallback)
        DeviceDriver.addOnGetCommandCompletedListener(getCommandCompletedListener)
        setContentView(binding.root)
        setDeviceState(RecordState.Invalid)
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
        locationAccuracy = 100.0
        locationCode = "UNKNOWN"
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
            rangerName = extras?.getString(EXTRA_RANGER_NAME, "")?.trim() ?: ""
            if (rangerName.isEmpty()) {
                showModalAlert(
                    getString(R.string.required),
                    getString(R.string.ranger_name_required),
                    ::goBack
                )
            } else {
                binding.toolbar.title = getString(R.string.getting_node_name)
                binding.toolbar.subtitle = getString(R.string.eloc_user, rangerName)
                binding.appVersionItem.valueText = App.versionName
                updateGpsViews()
                setListeners()
                connect()
            }
        }
    }

    private fun setStatusData() {
        binding.sessionIdItem.valueText = DeviceDriver.general.sessionId
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
        setDeviceState(DeviceDriver.general.recordingState)
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
        binding.connectionProgressIndicator.visibility = View.VISIBLE
        binding.connectionStatusTextView.setText(R.string.connecting)
        connectToDevice()
    }

    private fun setListeners() {
        DeviceDriver.registerConnectionChangedListener { onConnectionChanged(it) }
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.startRecordingButton.setOnClickListener { recordButtonClicked() }
        binding.stopButton.setOnClickListener { stopRecording() }
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
        binding.startDetectingButton.setOnClickListener {

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.scrollView.setOnScrollChangeListener(scrollChangeListener)
        }
    }

    private fun openSettings(showMicrophoneSection: Boolean = false) {
        if (recordingState.isIdle) {
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
                    DeviceDriver.getDeviceInfo()
                }

                ConnectionStatus.Inactive -> {
                    binding.swipeRefreshLayout.isEnabled = true
                    toggleContent(false)
                    binding.connectionProgressIndicator.visibility = View.GONE
                    binding.connectionStatusTextView.setText(R.string.disconnected_swipe_to_reconnect)
                }

                ConnectionStatus.Pending -> {
                    binding.swipeRefreshLayout.isEnabled = false
                    toggleContent(false)
                    binding.connectionProgressIndicator.visibility = View.VISIBLE
                    binding.connectionStatusTextView.setText(R.string.connecting)
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

    private fun confirmIgnoreAccuracy() {
        showModalOptionAlert(
            getString(R.string.confirm),
            getString(R.string.please_wait_for_gps_accuracy),
            getString(R.string.record_anyway),
            getString(R.string.wait55),
            { startRecording() },
        )
    }

    private fun recordButtonClicked() {
        // todo fix after proper states
        // this will not workk well until we get a proper enum or table of all states from fw devs
        // for now, i will work with the 'isIdle' property.
        if (!recordingState.isIdle) {
            stopRecording()
        } else {
            if (hasSDCardError) {
                showSDCardError()
                return
            }

            if (locationAccuracy <= 8.1) {
                startRecording()
            } else {
                confirmIgnoreAccuracy()
            }
        }
    }

    private fun stopRecording() {
        DeviceDriver.stopRecording()
    }

    private fun startRecording() {
        DeviceDriver.startRecording(locationCode, locationAccuracy)
        /*

       // todo: check if bt state while recording is maintained as set by user

                // Respect the bt recording state setting
                boolean btOnWhenRecording = preferencesHelper.getBluetoothRecordingState();
                send("setGPS^" + locationCode + "#" + locationAccuracy);


                // If bt on ELOC must be off, it means app will lose connection; go back to main screen
                if (!btOnWhenRecording) {
                    String message = getString(R.string.connection_close_message);
                    ActivityHelper.INSTANCE.showAlert(this, message, false, this::onBackPressed);
                }*/
    }

    private fun setDeviceState(state: RecordState, errorMessage: String = "") {
        recordingState = state
        val err = errorMessage.trim()
        if (err.isNotEmpty()) {
            showModalAlert(getString(R.string.oops), err)
        }
    }
}