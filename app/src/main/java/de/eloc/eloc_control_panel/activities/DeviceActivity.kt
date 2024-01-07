package de.eloc.eloc_control_panel.activities

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.openlocationcode.OpenLocationCode
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityDeviceBinding
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.DeviceState
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.driver.ElocData
import de.eloc.eloc_control_panel.interfaces.BluetoothDeviceListener
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

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
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_RANGER_NAME = "ranger_name"
        private const val CMD_SET_RECORD_MODE = "setRecordMode"
    }

    private var hasSDCardError = false
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private var locationAccuracy = 100.0 // Start with very inaccurate value of 100 meters.
    private var locationCode = "UNKNOWN"
    private lateinit var binding: ActivityDeviceBinding
    private var deviceAddress = ""
    private var deviceName = ""
    private var rangerName = ""
    private var refreshing = false
    private val scrollChangeListener =
        View.OnScrollChangeListener { _, _, y, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (y <= 5)
        }

    private val bluetoothDeviceListener = object : BluetoothDeviceListener() {
        override fun onRead(data: ByteArray) {
            try {
                val json = String(data)
                runOnUiThread { parseResponse(json) }
            } catch (_: Exception) {

            }
        }
    }

    private var deviceState = DeviceState.Disabled
        set(value) {
            field = value
            // todo: update recording/detecting since labels correctly
            binding.stopButton.visibility = View.GONE
            binding.startRecordingButton.visibility = View.GONE
            binding.startDetectingButton.visibility = View.GONE
            binding.stopButton.isClickable = false
            binding.stopButton.isFocusable = false
            when (field) {
                DeviceState.Disabled, DeviceState.RecordOffDetectOff, DeviceState.RecordOff, DeviceState.Idle -> {
                    binding.startRecordingButton.visibility = View.VISIBLE
                    binding.startDetectingButton.visibility = View.VISIBLE
                }

                DeviceState.Continuous, DeviceState.RecordOn -> {
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
                else -> {}
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setDeviceState(DeviceState.Disabled)
        setLaunchers()
        initialize()
    }

    override fun onResume() {
        super.onResume()
        setConfigData()
        setStatusData()
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
            deviceName = extras?.getString(EXTRA_DEVICE_NAME, "")?.trim() ?: ""
            if (deviceName.isEmpty()) {
                showModalAlert(
                    getString(R.string.required),
                    getString(R.string.device_name_required),
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
                    binding.toolbar.title = deviceName
                    binding.toolbar.subtitle = getString(R.string.eloc_user, rangerName)
                    binding.appVersionItem.itemValue = App.versionName
                    updateGpsViews()
                    setListeners()
                    connect()
                }
            }
        }
    }

    private fun setLaunchers() {
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val extras = intent.extras
                    if (extras != null) {
                        val command = extras.getString(DeviceSettingsActivity.COMMAND, "")
                        if (command.isNotEmpty()) {
                            DeviceDriver.write(command)
                            binding.coordinator.showSnack(
                                getString(R.string.command_sent_successfully)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun parseResponse(json: String) {
        val cmdField = "\"${DeviceDriver.KEY_CMD}\""
        val isResponse = json.contains(cmdField)
        if (isResponse) {
            runOnUiThread {
                // Hide the progress indicator when data is received i.e., there is a connection
                binding.swipeRefreshLayout.isRefreshing = false
            }
            try {
                // Parse JSON in a try block...
                // parsing might fail if incomplete JSON is received
                // i.e., JSON might be cut during disconnecting or refreshing.
                val root = JSONObject(json)
                processJson(root)
            } catch (_: Exception) {

            }
        }
    }

    private fun processJson(root: JSONObject) {
        when (root.getString(DeviceDriver.KEY_CMD).lowercase()) {
            CMD_SET_RECORD_MODE.lowercase() -> {
                setRecordingMode(root)
                DeviceDriver.getStatus()
            }

            DeviceDriver.CMD_GET_CONFIG.lowercase() -> {
                ElocData.parseConfig(root)
                setConfigData()
            }

            DeviceDriver.CMD_GET_STATUS.lowercase() -> {
                ElocData.parseStatus(root)
                setStatusData()
            }

            else -> {}
        }
    }

    private fun setStatusData() {

        binding.sessionIdItem.itemValue = ElocData.sessionId
        binding.recSinceBootItem.itemValue =
            TimeHelper.formatHours(this, ElocData.recHoursSinceBoot)
        binding.firmwareVersionItem.itemValue = ElocData.version
        binding.uptimeItem.itemValue = TimeHelper.formatHours(this, ElocData.uptimeHours)
        val level = ElocData.batteryLevel
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batteryGauge.updateValue(level)
        binding.batteryVoltItem.itemValue = formatNumber(ElocData.batteryVoltage, "V")
        binding.batteryTypeItem.itemValue = ElocData.batteryType
        val free = ElocData.freeSpaceGb
        binding.storageGauge.errorMode = (free <= 0)
        binding.storageStatus.text = if (binding.storageGauge.errorMode)
            ""
        else
            getString(R.string.gauge_free_space, free.toInt())
        binding.storageGauge.updateValue(ElocData.freeSpacePercentage)
        val gb = ElocData.sdCardSizeGb
        if (gb <= 0.0) {
            binding.storageTextView.text = getString(R.string.no_sd_card)
            binding.storageGauge.updateValue(0.0)
        } else {
            binding.storageTextView.text = formatNumber(gb, "GB")
        }
        setDeviceState(ElocData.deviceState)
    }

    private fun setConfigData() {
        val prettyRate =
            formatNumber(ElocData.sampleRate / 1000, "kHz", maxFractionDigits = 0)
        binding.sampleRateItem.itemValue = prettyRate
        binding.microphoneTypeItem.itemValue = ElocData.microphoneType
        binding.gainItem.itemValue = ElocData.microphoneGain.toString()
        binding.timePerFileItem.itemValue =
            TimeHelper.formatSeconds(this, ElocData.secondsPerFile, useSeconds = true)
        binding.lastLocationItem.itemValue = ElocData.lastLocation
        val enabledLabel =
            getString(if (ElocData.bluetoothEnabledDuringRecording) R.string.on else R.string.off)
        binding.bluetoothDuringRecordingItem.itemValue = enabledLabel
        binding.fileHeaderItem.itemValue = ElocData.fileHeader
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

    private fun openSettings() {
        // todo: fix after determining proper states
        if (deviceState.isIdle) {
            val intent = Intent(this, DeviceSettingsActivity::class.java)
            // todo: device name might be changed in DeviceSettingsActivity
            intent.putExtra(EXTRA_DEVICE_NAME, deviceName)
            settingsLauncher.launch(intent)
        } else {
            showModalAlert(
                getString(R.string.not_available),
                getString(R.string.device_settings_not_available)
            )
        }
    }

    private fun connectToDevice() {
        DeviceDriver.connect(deviceAddress, bluetoothDeviceListener)
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

    private fun setRecordingMode(jsonObject: JSONObject) {
        ElocData.parseDeviceState(jsonObject)
        setDeviceState(ElocData.deviceState)
    }

    private fun formatNumber(number: Double, units: String, maxFractionDigits: Int = 2): String {
        val format = NumberFormat.getInstance(Locale.ENGLISH)
        format.maximumFractionDigits = maxFractionDigits
        format.minimumFractionDigits = 0
        return format.format(number) + units
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
        if (!deviceState.isIdle) {
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

    private fun setDeviceState(state: DeviceState, errorMessage: String = "") {
        deviceState = state
        val err = errorMessage.trim()
        if (err.isNotEmpty()) {
            showModalAlert(getString(R.string.oops), err)
        }
    }
}