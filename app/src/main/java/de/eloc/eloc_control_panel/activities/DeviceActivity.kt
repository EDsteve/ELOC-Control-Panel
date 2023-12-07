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
import de.eloc.eloc_control_panel.data.DeviceDriver
import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.DeviceState
import de.eloc.eloc_control_panel.data.GainType
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
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

        private const val PATH_SEPARATOR = ":"
        private const val KEY_CMD = "cmd"
        private const val KEY_MICROPHONE_GAIN = "MicBitShift"
        private const val KEY_MICROPHONE_TYPE = "MicType"
        private const val KEY_MICROPHONE_SAMPLE_RATE = "MicSampleRate"
        private const val KEY_SECONDS_PER_FILE = "secondsPerFile"
        private const val KEY_MICROPHONE = "mic"
        private const val KEY_CONFIG = "config"
        private const val KEY_SESSION = "session"
        private const val KEY_DEVICE = "device"
        private const val KEY_RECORDING_TIME = "recordingTime[h]"
        private const val KEY_UPTIME = "Uptime[h]"
        private const val KEY_FILE_HEADER = "fileHeader"
        private const val KEY_TOTAL_RECORDING_TIME = "totalRecordingTime[h]"
        private const val KEY_BLUETOOTH_ENABLED_DURING_REC = "bluetoothEnableDuringRecord"
        private const val KEY_LOCATION_CODE = "locationCode"
        private const val KEY_SDCARD_SIZE = "SdCardSize[GB]"
        private const val KEY_SDCARD_FREE_GB = "SdCardFreeSpace[GB]"
        private const val KEY_SDCARD_FREE_PERC = "SdCardFreeSpace[%]"
        private const val KEY_BATTERY = "battery"
        private const val KEY_TYPE = "type"
        private const val KEY_BATTERY_LEVEL = "SoC[%]"
        private const val KEY_VOLTAGE = "voltage[V]"
        private const val KEY_IDENTIFIER = "identifier"
        private const val KEY_PAYLOAD = "payload"
        private const val KEY_ECODE = "ecode"
        private const val KEY_STATE = "state"
        private const val KEY_RECORDING_STATE = "recordingState"
        private const val KEY_FIRMWARE = "firmware"
        private const val CMD_GET_CONFIG = "getConfig"
        private const val CMD_GET_STATUS = "getStatus"
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
    private var recordingSeconds = 0.0
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
                DeviceState.Disabled, DeviceState.RecordOffDetectOff, DeviceState.RecordOff -> {
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
        startLocationUpdates()
        //connect()
        // todo: set appropritate state for device on connection
        // todo: make sure second call to connect() does not cause crash
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
        val cmdField = "\"$KEY_CMD\""
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
        val command = root.getString(KEY_CMD).lowercase()
        when (command) {
            CMD_SET_RECORD_MODE.lowercase() -> {
                setRecordingMode(root)
                DeviceDriver.getStatus()
            }

            CMD_GET_CONFIG.lowercase() -> {
                setMicrophoneType(root)
                setMicrophoneGain(root)
                setMicrophoneSampleRate(root)
                setHoursPerFile(root)
                setLastLocation(root)
                setBluetoothDuringRecording(root)
                setFileHeader(root)
            }

            CMD_GET_STATUS.lowercase() -> {
                setSessionID(root)
                setRecordingTime(root)
                setRecSinceBoot(root)
                setFirmwareVersion(root)
                setUptime(root)
                setCurrentBatteryLevel(root)
                setBatteryVoltage(root)
                setBatteryType(root)
                setSDCardFreeGB(root)
                setSDCardFreePerc(root)
                setSDCardSize(root)
                updateUi(root)
            }

            else -> {}
        }
    }

    private fun connect() {
        binding.toolbar.visibility = View.GONE
        binding.startDetectingButton.visibility = View.GONE
        binding.startRecordingButton.visibility = View.GONE
        binding.contentScrollView.visibility = View.GONE
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
            binding.contentScrollView.setOnScrollChangeListener(scrollChangeListener)
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
                    binding.toolbar.visibility = View.VISIBLE
                    binding.startDetectingButton.visibility = View.VISIBLE
                    binding.startRecordingButton.visibility = View.VISIBLE
                    binding.contentScrollView.visibility = View.VISIBLE
                    binding.loadingLinearLayout.visibility = View.GONE
                    binding.swipeRefreshLayout.isEnabled = true
                    menuInflater.inflate(R.menu.app_bar_settings, binding.toolbar.menu)
                    getDeviceInfo()
                }

                ConnectionStatus.Inactive -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.startDetectingButton.visibility = View.GONE
                    binding.startRecordingButton.visibility = View.GONE
                    binding.contentScrollView.visibility = View.GONE
                    binding.loadingLinearLayout.visibility = View.VISIBLE
                    binding.connectionProgressIndicator.visibility = View.GONE
                    binding.connectionStatusTextView.setText(R.string.disconnected_swipe_to_reconnect)
                    binding.swipeRefreshLayout.isEnabled = true
                }

                ConnectionStatus.Pending -> {
                    binding.toolbar.visibility = View.GONE
                    binding.startDetectingButton.visibility = View.GONE
                    binding.startRecordingButton.visibility = View.GONE
                    binding.contentScrollView.visibility = View.GONE
                    binding.loadingLinearLayout.visibility = View.VISIBLE
                    binding.connectionProgressIndicator.visibility = View.VISIBLE
                    binding.connectionStatusTextView.setText(R.string.connecting)
                    binding.swipeRefreshLayout.isEnabled = false
                }
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

    private fun setMicrophoneSampleRate(jsonObject: JSONObject) {
        // Sample rate
        val sampleRatePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_SAMPLE_RATE"
        val sampleRate = getJSONNumberAttribute(sampleRatePath, jsonObject)
        val prettyRate = formatNumber(sampleRate / 1000, "kHz", maxFractionDigits = 0)
        binding.sampleRateItem.itemValue = prettyRate
    }

    private fun setHoursPerFile(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_SECONDS_PER_FILE"
        val secondsPerFile = getJSONNumberAttribute(path, jsonObject)
        val hoursPerFile = TimeHelper.toHours(secondsPerFile)
        binding.hoursPerFileItem.itemValue = formatNumber(hoursPerFile, "h")
    }

    private fun setLastLocation(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_LOCATION_CODE"
        val location = getJSONStringAttribute(path, jsonObject)
        binding.lastLocationItem.itemValue = location
    }

    private fun setFileHeader(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_FILE_HEADER"
        val fileHeader = getJSONStringAttribute(path, jsonObject)
        binding.fileHeaderItem.itemValue = fileHeader
    }

    private fun setBluetoothDuringRecording(jsonObject: JSONObject) {
        val path =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_CONFIG$PATH_SEPARATOR$KEY_BLUETOOTH_ENABLED_DURING_REC"
        val enabled = getJSONBooleanAttribute(path, jsonObject)
        val enabledLabel = getString(if (enabled) R.string.on else R.string.off)
        binding.bluetoothDuringRecordingItem.itemValue = enabledLabel
    }

    private fun setSessionID(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_IDENTIFIER"
        val sessionId = getJSONStringAttribute(path, jsonObject)
        binding.sessionIdItem.itemValue = sessionId
    }

    private fun setRecordingTime(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_TIME"
        val hours = getJSONNumberAttribute(path, jsonObject)
        recordingSeconds = TimeHelper.toSeconds(hours)
    }

    private fun setRecSinceBoot(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_TOTAL_RECORDING_TIME"
        val hours = getJSONNumberAttribute(path, jsonObject)
        binding.recSinceBootItem.itemValue = TimeHelper.formatHours(this, hours)
    }

    private fun setFirmwareVersion(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_FIRMWARE"
        val version = getJSONStringAttribute(path, jsonObject)
        binding.firmwareVersionItem.itemValue = version
    }

    private fun setUptime(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_UPTIME"
        val hours = getJSONNumberAttribute(path, jsonObject)
        val time = TimeHelper.formatHours(this, hours)
        binding.uptimeItem.itemValue = time
    }

    private fun setBatteryVoltage(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_VOLTAGE"
        val volts = getJSONNumberAttribute(path, jsonObject)
        binding.batteryVoltItem.itemValue = formatNumber(volts, "V")
    }

    private fun setCurrentBatteryLevel(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_BATTERY_LEVEL"
        val level = getJSONNumberAttribute(path, jsonObject)
        binding.batteryStatus.text = getString(R.string.gauge_battery_level, level.toInt())
        binding.batteryGauge.updateValue(level)
    }

    private fun setBatteryType(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_BATTERY$PATH_SEPARATOR$KEY_TYPE"
        val batteryType = getJSONStringAttribute(path, jsonObject)
        binding.batteryTypeItem.itemValue = batteryType
    }

    private fun setSDCardSize(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_SIZE"
        val gb = getJSONNumberAttribute(path, jsonObject)
        if (gb <= 0.0) {
            binding.storageTextView.text = getString(R.string.no_sd_card)
            binding.storageGauge.updateValue(0.0)
        } else {
            binding.storageTextView.text = formatNumber(gb, "GB")
        }
    }

    private fun updateUi(jsonObject: JSONObject) {
        val path =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_SESSION$PATH_SEPARATOR$KEY_RECORDING_STATE$PATH_SEPARATOR$KEY_STATE"
        val raw = getJSONStringAttribute(path, jsonObject).lowercase()
        val state = DeviceState.parse(raw)
        setDeviceState(state)
    }

    private fun setSDCardFreePerc(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_FREE_PERC"
        val free = getJSONNumberAttribute(path, jsonObject)
        binding.storageGauge.updateValue(free)
    }

    private fun setSDCardFreeGB(jsonObject: JSONObject) {
        val path = "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_DEVICE$PATH_SEPARATOR$KEY_SDCARD_FREE_GB"
        val free = getJSONNumberAttribute(path, jsonObject)
        if (free <= 0) {
            binding.storageStatus.text = ""
            binding.storageErrorIcon.visibility = View.VISIBLE
        } else {
            binding.storageStatus.text = getString(R.string.gauge_free_space, free.toInt())
            binding.storageErrorIcon.visibility = View.INVISIBLE
        }

    }

    private fun setRecordingMode(jsonObject: JSONObject) {
        val resultCode = getJSONNumberAttribute(KEY_ECODE, jsonObject).toInt()
        val commandSucceeded = resultCode == 0
        if (commandSucceeded) {
            val raw = getJSONStringAttribute(
                "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_RECORDING_STATE",
                jsonObject
            )
            val state = DeviceState.parse(raw)
            setDeviceState(state)
        }
    }

    private fun setMicrophoneType(jsonObject: JSONObject) {
        val typePath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_TYPE"
        val micType = getJSONStringAttribute(typePath, jsonObject)
        binding.microphoneTypeItem.itemValue = micType
    }

    private fun setMicrophoneGain(jsonObject: JSONObject) {
        val gainPath =
            "$KEY_PAYLOAD$PATH_SEPARATOR$KEY_MICROPHONE$PATH_SEPARATOR$KEY_MICROPHONE_GAIN"
        val micBitShift = getJSONStringAttribute(gainPath, jsonObject)
        val gainText = when (GainType.fromValue(micBitShift)) {
            GainType.Low -> getString(R.string.low)
            GainType.High -> getString(R.string.high)
        }
        binding.gainItem.itemValue = gainText
    }

    private fun formatNumber(number: Double, units: String, maxFractionDigits: Int = 2): String {
        val format = NumberFormat.getInstance(Locale.ENGLISH)
        format.maximumFractionDigits = maxFractionDigits
        format.minimumFractionDigits = 0
        return format.format(number) + units
    }

    private fun getDeviceInfo() {
        DeviceDriver.getStatus()
        DeviceDriver.getConfig()
    }

    private fun getJSONBooleanAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: Boolean = false,
    ): Boolean {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getBoolean(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    private fun getJSONStringAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: String = ""
    ): String {
        var attribute = defaultValue
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            attribute = node?.getString(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return attribute
    }

    private fun getJSONNumberAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: Double = 0.0
    ): Double {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getDouble(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    private fun getJSONAttributeNode(
        path: String,
        jsonObject: JSONObject
    ): Pair<String, JSONObject?> {
        var result: JSONObject? = null
        var key = ""
        try {
            val parts = path.split(PATH_SEPARATOR)
            val iterator = parts.iterator()
            var node = jsonObject
            while (iterator.hasNext()) {
                val name = iterator.next()
                if (!iterator.hasNext()) {
                    result = node
                    key = name
                } else {
                    node = node.getJSONObject(name)
                }
            }
        } catch (_: Exception) {

        }
        return (key to result)
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